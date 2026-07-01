package com.neusoft.hospital.service;

import com.neusoft.hospital.auth.enums.Role;
import com.neusoft.hospital.common.PageResult;
import com.neusoft.hospital.dto.request.AccountCreateEmployeeRequest;
import com.neusoft.hospital.dto.request.AccountCreatePatientRequest;
import com.neusoft.hospital.dto.request.AccountStatusUpdateRequest;
import com.neusoft.hospital.dto.request.ResetPasswordRequest;
import com.neusoft.hospital.dto.response.AccountResponse;

/**
 * 账号管理服务（PR5，仅 ADMIN 调用）。
 * <p>
 * 创建/启禁用/重置密码均会递增 user_account.token_version，使该账号所有历史 Token 立即失效。
 * 不返回 password/hash 与患者敏感字段；不信任前端传入的 accountId/tokenVersion/status/delmark。
 */
public interface AccountService {

    PageResult<AccountResponse> pageAccounts(Integer pageNum, Integer pageSize, Role role, Integer status, String usernameKeyword);

    AccountResponse getAccount(Integer accountId);

    /** 创建员工账号(ADMIN/DOCTOR)，绑定现有 employee。 */
    AccountResponse createEmployeeAccount(AccountCreateEmployeeRequest request);

    /** 创建患者账号(PATIENT)，绑定现有 patient。 */
    AccountResponse createPatientAccount(AccountCreatePatientRequest request);

    /** 启用/禁用账号；status 变更时 token_version+1。 */
    void updateStatus(Integer accountId, AccountStatusUpdateRequest request);

    /** 重置账号密码；token_version+1。 */
    void resetPassword(Integer accountId, ResetPasswordRequest request);
}
