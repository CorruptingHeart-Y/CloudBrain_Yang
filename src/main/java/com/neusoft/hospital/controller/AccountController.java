package com.neusoft.hospital.controller;

import com.neusoft.hospital.auth.annotation.RequireRole;
import com.neusoft.hospital.auth.enums.Role;
import com.neusoft.hospital.common.PageResult;
import com.neusoft.hospital.common.Result;
import com.neusoft.hospital.dto.request.AccountCreateEmployeeRequest;
import com.neusoft.hospital.dto.request.AccountCreatePatientRequest;
import com.neusoft.hospital.dto.request.AccountStatusUpdateRequest;
import com.neusoft.hospital.dto.request.ResetPasswordRequest;
import com.neusoft.hospital.dto.response.AccountResponse;
import com.neusoft.hospital.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "账号管理", description = "ADMIN 创建/查询/启禁用/重置密码 账号（仅 ADMIN）")
@RestController
@RequestMapping("/api/v1/account")
@RequiredArgsConstructor
@RequireRole(Role.ADMIN)
public class AccountController {

    private final AccountService accountService;

    @Operation(summary = "分页查询账号列表", description = "可按 role/status/username 关键字筛选；不返回密码及患者敏感字段",
            security = @SecurityRequirement(name = "Bearer"))
    @GetMapping
    public Result<PageResult<AccountResponse>> page(
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数", example = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "角色") @RequestParam(required = false) Role role,
            @Parameter(description = "状态：1-启用 0-禁用") @RequestParam(required = false) Integer status,
            @Parameter(description = "账号关键字(模糊)") @RequestParam(required = false) String username) {
        return Result.ok(accountService.pageAccounts(pageNum, pageSize, role, status, username));
    }

    @Operation(summary = "查询账号详情", description = "不返回密码及患者敏感字段", security = @SecurityRequirement(name = "Bearer"))
    @GetMapping("/{accountId}")
    public Result<AccountResponse> get(@Parameter(description = "账号ID", required = true) @PathVariable Integer accountId) {
        return Result.ok(accountService.getAccount(accountId));
    }

    @Operation(summary = "创建员工账号", description = "仅 ADMIN/DOCTOR；绑定现有且未绑定的 employee；BCrypt 入库",
            security = @SecurityRequirement(name = "Bearer"))
    @PostMapping("/employee")
    public Result<AccountResponse> createEmployee(@RequestBody @Valid AccountCreateEmployeeRequest request) {
        return Result.ok(accountService.createEmployeeAccount(request));
    }

    @Operation(summary = "创建患者账号", description = "固定 PATIENT；绑定现有且未绑定的 patient；不返回敏感字段",
            security = @SecurityRequirement(name = "Bearer"))
    @PostMapping("/patient")
    public Result<AccountResponse> createPatient(@RequestBody @Valid AccountCreatePatientRequest request) {
        return Result.ok(accountService.createPatientAccount(request));
    }

    @Operation(summary = "启用/禁用账号", description = "不能禁用自己/最后一个启用管理员；status 变更后旧 Token 立即失效",
            security = @SecurityRequirement(name = "Bearer"))
    @PatchMapping("/{accountId}/status")
    public Result<Void> updateStatus(@Parameter(description = "账号ID", required = true) @PathVariable Integer accountId,
                                     @RequestBody @Valid AccountStatusUpdateRequest request) {
        accountService.updateStatus(accountId, request);
        return Result.ok();
    }

    @Operation(summary = "重置账号密码", description = "BCrypt 入库；token_version+1，旧 Token 立即失效；不返回新密码",
            security = @SecurityRequirement(name = "Bearer"))
    @PostMapping("/{accountId}/reset-password")
    public Result<Void> resetPassword(@Parameter(description = "账号ID", required = true) @PathVariable Integer accountId,
                                      @RequestBody @Valid ResetPasswordRequest request) {
        accountService.resetPassword(accountId, request);
        return Result.ok();
    }
}
