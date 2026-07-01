package com.neusoft.hospital.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.hospital.auth.context.AuthUser;
import com.neusoft.hospital.auth.context.CurrentUser;
import com.neusoft.hospital.auth.enums.Role;
import com.neusoft.hospital.common.BusinessException;
import com.neusoft.hospital.common.ErrorCode;
import com.neusoft.hospital.common.PageResult;
import com.neusoft.hospital.dto.request.AccountCreateEmployeeRequest;
import com.neusoft.hospital.dto.request.AccountCreatePatientRequest;
import com.neusoft.hospital.dto.request.AccountStatusUpdateRequest;
import com.neusoft.hospital.dto.request.ResetPasswordRequest;
import com.neusoft.hospital.dto.response.AccountResponse;
import com.neusoft.hospital.entity.Employee;
import com.neusoft.hospital.entity.Patient;
import com.neusoft.hospital.entity.UserAccount;
import com.neusoft.hospital.mapper.PatientMapper;
import com.neusoft.hospital.mapper.UserAccountMapper;
import com.neusoft.hospital.service.AccountService;
import com.neusoft.hospital.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder();

    private final UserAccountMapper userAccountMapper;
    private final EmployeeService employeeService;
    private final PatientMapper patientMapper;

    @Override
    public PageResult<AccountResponse> pageAccounts(Integer pageNum, Integer pageSize, Role role, Integer status, String usernameKeyword) {
        int pNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int pSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        LambdaQueryWrapper<UserAccount> wrapper = new LambdaQueryWrapper<>();
        if (role != null) {
            wrapper.eq(UserAccount::getRole, role.name());
        }
        if (status != null) {
            wrapper.eq(UserAccount::getStatus, status);
        }
        if (StringUtils.hasText(usernameKeyword)) {
            wrapper.like(UserAccount::getUsername, usernameKeyword);
        }
        wrapper.orderByDesc(UserAccount::getId);
        Page<UserAccount> page = new Page<>(pNum, pSize);
        List<AccountResponse> records = userAccountMapper.selectPage(page, wrapper).getRecords()
                .stream().map(this::toResponse).toList();
        return PageResult.of(page.getTotal(), pNum, pSize, records);
    }

    @Override
    public AccountResponse getAccount(Integer accountId) {
        UserAccount account = userAccountMapper.selectById(accountId);
        if (account == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return toResponse(account);
    }

    @Override
    public AccountResponse createEmployeeAccount(AccountCreateEmployeeRequest request) {
        // 仅 ADMIN/DOCTOR，禁止 PATIENT
        if (request.getRole() == Role.PATIENT) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "员工账号角色仅可为 ADMIN 或 DOCTOR");
        }
        // employee 必须存在
        Employee employee = employeeService.getById(request.getEmployeeId());
        if (employee == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        // employeeId 不得已绑定其他账号
        if (userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccount>()
                .eq(UserAccount::getEmployeeId, request.getEmployeeId())) != null) {
            throw new BusinessException(ErrorCode.CONFLICT.getCode(), "该员工已绑定账号");
        }
        ensureUsernameFree(request.getUsername());

        UserAccount account = new UserAccount();
        account.setUsername(request.getUsername());
        account.setPassword(BCRYPT.encode(request.getPassword()));
        account.setRole(request.getRole().name());
        account.setEmployeeId(request.getEmployeeId());
        account.setPatientId(null);
        account.setStatus(1);
        account.setTokenVersion(1);
        account.setDelmark(1);
        userAccountMapper.insert(account);
        return toResponse(userAccountMapper.selectById(account.getId()));
    }

    @Override
    public AccountResponse createPatientAccount(AccountCreatePatientRequest request) {
        Patient patient = patientMapper.selectById(request.getPatientId());
        if (patient == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccount>()
                .eq(UserAccount::getPatientId, request.getPatientId())) != null) {
            throw new BusinessException(ErrorCode.CONFLICT.getCode(), "该患者已绑定账号");
        }
        ensureUsernameFree(request.getUsername());

        UserAccount account = new UserAccount();
        account.setUsername(request.getUsername());
        account.setPassword(BCRYPT.encode(request.getPassword()));
        account.setRole(Role.PATIENT.name());
        account.setEmployeeId(null);
        account.setPatientId(request.getPatientId());
        account.setStatus(1);
        account.setTokenVersion(1);
        account.setDelmark(1);
        userAccountMapper.insert(account);
        return toResponse(userAccountMapper.selectById(account.getId()));
    }

    @Override
    public void updateStatus(Integer accountId, AccountStatusUpdateRequest request) {
        Integer newStatus = request.getStatus();
        if (newStatus == null || (newStatus != 0 && newStatus != 1)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "状态仅可为 0 或 1");
        }
        UserAccount account = userAccountMapper.selectById(accountId);
        if (account == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        // 状态未变更是幂等无操作
        if (newStatus.equals(account.getStatus())) {
            return;
        }

        AuthUser current = CurrentUser.requireAuthUser();
        // 禁止禁用自己
        if (newStatus == 0 && current.getAccountId() != null && current.getAccountId().equals(accountId)) {
            throw new BusinessException(ErrorCode.CONFLICT.getCode(), "不能禁用当前登录账号");
        }
        // 禁止禁用最后一个启用的 ADMIN
        if (newStatus == 0 && Role.ADMIN.name().equals(account.getRole()) && account.getStatus() == 1) {
            Long enabledAdmins = userAccountMapper.selectCount(new LambdaQueryWrapper<UserAccount>()
                    .eq(UserAccount::getRole, Role.ADMIN.name())
                    .eq(UserAccount::getStatus, 1));
            if (enabledAdmins != null && enabledAdmins <= 1) {
                throw new BusinessException(ErrorCode.CONFLICT.getCode(), "不能禁用最后一个启用的管理员");
            }
        }
        // status 变更 → token_version+1，旧 Token 立即失效（启用也不恢复旧 Token）
        userAccountMapper.update(null, new LambdaUpdateWrapper<UserAccount>()
                .eq(UserAccount::getId, accountId)
                .set(UserAccount::getStatus, newStatus)
                .setSql("token_version = token_version + 1"));
    }

    @Override
    public void resetPassword(Integer accountId, ResetPasswordRequest request) {
        UserAccount account = userAccountMapper.selectById(accountId);
        if (account == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        // 重置密码 → token_version+1，旧 Token 立即失效
        userAccountMapper.update(null, new LambdaUpdateWrapper<UserAccount>()
                .eq(UserAccount::getId, accountId)
                .set(UserAccount::getPassword, BCRYPT.encode(request.getNewPassword()))
                .setSql("token_version = token_version + 1"));
    }

    private void ensureUsernameFree(String username) {
        if (userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccount>()
                .eq(UserAccount::getUsername, username)) != null) {
            throw new BusinessException(ErrorCode.CONFLICT.getCode(), "账号已存在");
        }
    }

    private AccountResponse toResponse(UserAccount account) {
        AccountResponse r = new AccountResponse();
        r.setAccountId(account.getId());
        r.setUsername(account.getUsername());
        r.setRole(Role.fromString(account.getRole()));
        r.setStatus(account.getStatus());
        r.setEmployeeId(account.getEmployeeId());
        r.setPatientId(account.getPatientId());
        r.setTokenVersion(account.getTokenVersion());
        r.setDisplayName(resolveDisplayName(account));
        r.setCreatedTime(account.getCreateTime());
        r.setUpdatedTime(account.getUpdateTime());
        return r;
    }

    private String resolveDisplayName(UserAccount account) {
        if (account.getEmployeeId() != null) {
            Employee emp = employeeService.getById(account.getEmployeeId());
            if (emp != null) {
                return emp.getRealname();
            }
        }
        if (account.getPatientId() != null) {
            Patient pat = patientMapper.selectById(account.getPatientId());
            if (pat != null) {
                return pat.getRealName();
            }
        }
        return null;
    }
}
