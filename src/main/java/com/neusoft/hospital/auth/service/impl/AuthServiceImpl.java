package com.neusoft.hospital.auth.service.impl;

import com.neusoft.hospital.auth.context.CurrentUser;
import com.neusoft.hospital.auth.dto.ChangePasswordRequest;
import com.neusoft.hospital.auth.dto.LoginRequest;
import com.neusoft.hospital.auth.dto.LoginResponse;
import com.neusoft.hospital.auth.dto.UserInfoResponse;
import com.neusoft.hospital.auth.jwt.JwtUtil;
import com.neusoft.hospital.auth.service.AuthService;
import com.neusoft.hospital.common.BusinessException;
import com.neusoft.hospital.common.ErrorCode;
import com.neusoft.hospital.entity.Department;
import com.neusoft.hospital.entity.Employee;
import com.neusoft.hospital.entity.RegistLevel;
import com.neusoft.hospital.service.DepartmentService;
import com.neusoft.hospital.service.EmployeeService;
import com.neusoft.hospital.service.RegistLevelService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder();
    private static final String FAIL_KEY_PREFIX = "login:fail:";
    private static final String BLACKLIST_KEY_PREFIX = "jwt:blacklist:";

    private final EmployeeService employeeService;
    private final DepartmentService departmentService;
    private final RegistLevelService registLevelService;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redis;

    @Value("${hospital.login.max-fail:5}")
    private int maxFail;

    @Value("${hospital.login.lock-minutes:15}")
    private int lockMinutes;

    @Override
    public LoginResponse login(LoginRequest request) {
        Integer id = request.getId();
        String failKey = FAIL_KEY_PREFIX + id;
        String failCountStr = redis.opsForValue().get(failKey);
        int failCount = failCountStr == null ? 0 : Integer.parseInt(failCountStr);
        if (failCount >= maxFail) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }

        Employee employee = employeeService.getById(id);
        if (employee == null) {
            recordFailure(failKey);
            throw new BusinessException(ErrorCode.BAD_CREDENTIALS);
        }

        if (!matchesPassword(request.getPassword(), employee.getPassword())) {
            recordFailure(failKey);
            throw new BusinessException(ErrorCode.BAD_CREDENTIALS);
        }

        // 旧 MD5 验证通过 → 自动升级 BCrypt
        if (isMd5(employee.getPassword())) {
            String upgraded = BCRYPT.encode(request.getPassword());
            Employee update = new Employee();
            update.setId(employee.getId());
            update.setPassword(upgraded);
            employeeService.updateById(update);
        }

        redis.delete(failKey);

        String token = jwtUtil.generate(id);
        LoginResponse resp = new LoginResponse();
        resp.setToken(token);
        resp.setExpiresAt(System.currentTimeMillis() + jwtUtil.getExpireMillis());
        resp.setUserInfo(buildUserInfo(employee));
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
        Integer id = CurrentUser.require();
        Employee employee = employeeService.getById(id);
        if (employee == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return buildUserInfo(employee);
    }

    @Override
    public void changePassword(ChangePasswordRequest request) {
        Integer id = CurrentUser.require();
        Employee employee = employeeService.getById(id);
        if (employee == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (!matchesPassword(request.getOldPassword(), employee.getPassword())) {
            throw new BusinessException(ErrorCode.BAD_CREDENTIALS);
        }
        Employee update = new Employee();
        update.setId(id);
        update.setPassword(BCRYPT.encode(request.getNewPassword()));
        employeeService.updateById(update);
    }

    private UserInfoResponse buildUserInfo(Employee employee) {
        UserInfoResponse info = new UserInfoResponse();
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
