package com.neusoft.hospital.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neusoft.hospital.auth.context.AuthUser;
import com.neusoft.hospital.auth.context.CurrentUser;
import com.neusoft.hospital.auth.dto.ChangePasswordRequest;
import com.neusoft.hospital.auth.dto.LoginRequest;
import com.neusoft.hospital.auth.dto.LoginResponse;
import com.neusoft.hospital.auth.dto.PatientRegisterRequest;
import com.neusoft.hospital.auth.dto.UserInfoResponse;
import com.neusoft.hospital.auth.enums.Role;
import com.neusoft.hospital.auth.jwt.JwtUtil;
import com.neusoft.hospital.auth.service.AuthService;
import com.neusoft.hospital.common.BusinessException;
import com.neusoft.hospital.common.ErrorCode;
import com.neusoft.hospital.entity.Department;
import com.neusoft.hospital.entity.Employee;
import com.neusoft.hospital.entity.Patient;
import com.neusoft.hospital.entity.RegistLevel;
import com.neusoft.hospital.entity.UserAccount;
import com.neusoft.hospital.mapper.PatientMapper;
import com.neusoft.hospital.mapper.UserAccountMapper;
import com.neusoft.hospital.service.DepartmentService;
import com.neusoft.hospital.service.EmployeeService;
import com.neusoft.hospital.service.RegistLevelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder();
    private static final String FAIL_KEY_PREFIX = "login:fail:";
    private static final String BLACKLIST_KEY_PREFIX = "jwt:blacklist:";

    private final EmployeeService employeeService;
    private final DepartmentService departmentService;
    private final RegistLevelService registLevelService;
    private final UserAccountMapper userAccountMapper;
    private final PatientMapper patientMapper;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redis;

    @Value("${hospital.login.max-fail:5}")
    private int maxFail;

    @Value("${hospital.login.lock-minutes:15}")
    private int lockMinutes;

    @Override
    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername();
        String failKey = FAIL_KEY_PREFIX + username;
        String failCountStr = redis.opsForValue().get(failKey);
        int failCount = failCountStr == null ? 0 : Integer.parseInt(failCountStr);
        if (failCount >= maxFail) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }

        UserAccount account = userAccountMapper.selectOne(
                new LambdaQueryWrapper<UserAccount>().eq(UserAccount::getUsername, username));
        // 不存在 / 已逻辑删除(TableLogic 自动过滤)：统一安全失败，不泄露具体原因
        if (account == null) {
            recordFailure(failKey);
            throw new BusinessException(ErrorCode.BAD_CREDENTIALS);
        }
        // 禁用账号：同样统一安全失败
        if (account.getStatus() == null || account.getStatus() != 1) {
            recordFailure(failKey);
            throw new BusinessException(ErrorCode.BAD_CREDENTIALS);
        }

        if (!matchesPassword(request.getPassword(), account.getPassword())) {
            recordFailure(failKey);
            throw new BusinessException(ErrorCode.BAD_CREDENTIALS);
        }

        // 旧 MD5 验证通过 → 立即升级该 user_account.password 为 BCrypt（不再改写 employee.password）
        if (isMd5(account.getPassword())) {
            UserAccount upgrade = new UserAccount();
            upgrade.setId(account.getId());
            upgrade.setPassword(BCRYPT.encode(request.getPassword()));
            userAccountMapper.updateById(upgrade);
            log.info("user_account.id={} 密码已由 MD5 升级为 BCrypt", account.getId());
        }

        redis.delete(failKey);

        Role role = Role.fromString(account.getRole());
        // 角色关联校验：拒绝配置不合法的账号登录，记录清晰服务端日志，对外仍安全失败
        String realname = resolveRealnameAndValidate(account, role);

        AuthUser authUser = AuthUser.builder()
                .accountId(account.getId())
                .role(role)
                .employeeId(account.getEmployeeId())
                .patientId(account.getPatientId())
                .tokenVersion(account.getTokenVersion())
                .realname(realname)
                .build();

        String token = jwtUtil.generate(authUser);
        LoginResponse resp = new LoginResponse();
        resp.setToken(token);
        resp.setExpiresAt(System.currentTimeMillis() + jwtUtil.getExpireMillis());
        resp.setUserInfo(buildUserInfo(account, role));
        return resp;
    }

    @Override
    public void logout(String token) {
        long remaining = jwtUtil.remainingSeconds(token);
        if (remaining <= 0) return;
        redis.opsForValue().set(BLACKLIST_KEY_PREFIX + token, "1", Duration.ofSeconds(remaining));
    }

    @Override
    public UserInfoResponse currentUserInfo() {
        AuthUser user = CurrentUser.requireAuthUser();
        UserAccount account = userAccountMapper.selectById(user.getAccountId());
        if (account == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return buildUserInfo(account, user.getRole());
    }

    @Override
    public void changePassword(ChangePasswordRequest request) {
        AuthUser user = CurrentUser.requireAuthUser();
        UserAccount account = userAccountMapper.selectById(user.getAccountId());
        if (account == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (!matchesPassword(request.getOldPassword(), account.getPassword())) {
            throw new BusinessException(ErrorCode.BAD_CREDENTIALS);
        }
        // 只改写 user_account.password（BCrypt），禁止继续改写 employee.password
        // PR5：同时 token_version+1，使当前及所有历史 Token 立即失效（需重新登录）
        userAccountMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<UserAccount>()
                .eq(UserAccount::getId, account.getId())
                .set(UserAccount::getPassword, BCRYPT.encode(request.getNewPassword()))
                .setSql("token_version = token_version + 1"));
    }

    @Override
    @Transactional
    public void registerPatient(PatientRegisterRequest request) {
        // 不把 cardNumber/手机号/地址/密码写入普通日志；失败统一安全消息，不泄露身份证是否存在
        try {
            Patient existing = patientMapper.selectOne(
                    new LambdaQueryWrapper<Patient>().eq(Patient::getCardNumber, request.getCardNumber()));
            if (existing == null) {
                // 新患者：建 patient + PATIENT 账号
                Patient p = new Patient();
                p.setRealName(request.getRealName());
                p.setGender(request.getGender());
                p.setCardNumber(request.getCardNumber());
                p.setBirthdate(request.getBirthdate());
                p.setPhone(request.getPhone());
                p.setHomeAddress(request.getHomeAddress());
                p.setDelmark(1);
                patientMapper.insert(p);
                insertPatientAccount(request.getUsername(), request.getPassword(), p.getId());
            } else {
                // 已有患者：仅当 realName/gender/birthdate 完全一致才允许绑定；不修改其关键身份资料
                if (!patientIdentityMatches(existing, request)) {
                    throw new BusinessException(ErrorCode.REGISTER_FAILED);
                }
                // 已绑定账号 / username 已占用 → 统一安全失败（不区分原因）
                if (userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccount>()
                        .eq(UserAccount::getPatientId, existing.getId())) != null) {
                    throw new BusinessException(ErrorCode.REGISTER_FAILED);
                }
                if (userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccount>()
                        .eq(UserAccount::getUsername, request.getUsername())) != null) {
                    throw new BusinessException(ErrorCode.REGISTER_FAILED);
                }
                insertPatientAccount(request.getUsername(), request.getPassword(), existing.getId());
            }
        } catch (DuplicateKeyException e) {
            // 并发兜底：uk_username / uk_patient_id / uk_card_number 冲突 → 统一安全失败，事务回滚无部分写入
            throw new BusinessException(ErrorCode.REGISTER_FAILED);
        }
    }

    private void insertPatientAccount(String username, String rawPassword, Integer patientId) {
        UserAccount account = new UserAccount();
        account.setUsername(username);
        account.setPassword(BCRYPT.encode(rawPassword));
        account.setRole(Role.PATIENT.name());
        account.setEmployeeId(null);
        account.setPatientId(patientId);
        account.setStatus(1);
        account.setTokenVersion(1);
        account.setDelmark(1);
        userAccountMapper.insert(account);
    }

    private boolean patientIdentityMatches(Patient p, PatientRegisterRequest r) {
        return Objects.equals(p.getRealName(), r.getRealName())
                && Objects.equals(p.getGender(), r.getGender())
                && Objects.equals(p.getBirthdate(), r.getBirthdate());
    }

    /**
     * 角色关联校验并解析展示用 realname。
     * - PATIENT 必须有合法 patientId（且 patient 存在）；
     * - DOCTOR 必须有合法 employeeId（且 employee 存在）；
     * - ADMIN 的 employeeId 可空。
     * 不合法则记录服务端日志并拒绝登录（对外安全失败）。
     */
    private String resolveRealnameAndValidate(UserAccount account, Role role) {
        switch (role) {
            case PATIENT: {
                if (account.getPatientId() == null) {
                    log.error("登录拒绝：PATIENT 账号 user_account.id={} 缺少 patientId", account.getId());
                    throw new BusinessException(ErrorCode.BAD_CREDENTIALS);
                }
                Patient patient = patientMapper.selectById(account.getPatientId());
                if (patient == null) {
                    log.error("登录拒绝：PATIENT 账号 user_account.id={} 的 patientId={} 无对应 patient",
                            account.getId(), account.getPatientId());
                    throw new BusinessException(ErrorCode.BAD_CREDENTIALS);
                }
                return patient.getRealName();
            }
            case DOCTOR: {
                if (account.getEmployeeId() == null) {
                    log.error("登录拒绝：DOCTOR 账号 user_account.id={} 缺少 employeeId", account.getId());
                    throw new BusinessException(ErrorCode.BAD_CREDENTIALS);
                }
                Employee employee = employeeService.getById(account.getEmployeeId());
                if (employee == null) {
                    log.error("登录拒绝：DOCTOR 账号 user_account.id={} 的 employeeId={} 无对应 employee",
                            account.getId(), account.getEmployeeId());
                    throw new BusinessException(ErrorCode.BAD_CREDENTIALS);
                }
                return employee.getRealname();
            }
            case ADMIN: {
                if (account.getEmployeeId() != null) {
                    Employee employee = employeeService.getById(account.getEmployeeId());
                    if (employee != null) {
                        return employee.getRealname();
                    }
                }
                return null;
            }
            default:
                return null;
        }
    }

    private UserInfoResponse buildUserInfo(UserAccount account, Role role) {
        UserInfoResponse info = new UserInfoResponse();
        info.setAccountId(account.getId());
        info.setRole(role);
        info.setEmployeeId(account.getEmployeeId());
        info.setPatientId(account.getPatientId());

        if (role == Role.PATIENT) {
            // 患者：仅展示 realname，医生专属字段保持 null，不返回身份证/地址/手机号等敏感字段
            if (account.getPatientId() != null) {
                Patient patient = patientMapper.selectById(account.getPatientId());
                if (patient != null) {
                    info.setRealname(patient.getRealName());
                }
            }
            // 旧字段 id(employeeId) 对患者为 null
            return info;
        }

        // ADMIN / DOCTOR：若有关联 employee 则返回 realname/科室/挂号级别；ADMIN 无关联则相关字段为 null，不报错
        if (account.getEmployeeId() != null) {
            Employee employee = employeeService.getById(account.getEmployeeId());
            if (employee != null) {
                info.setId(employee.getId());
                info.setRealname(employee.getRealname());
                info.setDeptmentId(employee.getDeptmentId());
                info.setRegistLevelId(employee.getRegistLevelId());
                if (employee.getDeptmentId() != null) {
                    Department dept = departmentService.getById(employee.getDeptmentId());
                    if (dept != null) info.setDeptName(dept.getDeptName());
                }
                if (employee.getRegistLevelId() != null) {
                    RegistLevel level = registLevelService.getById(employee.getRegistLevelId());
                    if (level != null) info.setRegistLevelName(level.getRegistName());
                }
            }
        }
        return info;
    }

    private void recordFailure(String failKey) {
        Long count = redis.opsForValue().increment(failKey);
        if (count != null && count == 1L) {
            redis.expire(failKey, Duration.ofMinutes(lockMinutes));
        }
    }

    private boolean matchesPassword(String raw, String stored) {
        if (stored == null || stored.isEmpty()) return false;
        if (isBcrypt(stored)) {
            return BCRYPT.matches(raw, stored);
        }
        if (isMd5(stored)) {
            String md5 = DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
            return md5.equalsIgnoreCase(stored);
        }
        return false;
    }

    private static boolean isBcrypt(String h) {
        return h.length() == 60 && (h.startsWith("$2a$") || h.startsWith("$2b$") || h.startsWith("$2y$"));
    }

    private static boolean isMd5(String h) {
        return h.length() == 32 && h.matches("^[0-9a-fA-F]{32}$");
    }
}
