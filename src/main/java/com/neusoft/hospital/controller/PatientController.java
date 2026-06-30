package com.neusoft.hospital.controller;

import com.neusoft.hospital.auth.annotation.RequireRole;
import com.neusoft.hospital.auth.enums.Role;
import com.neusoft.hospital.common.PageResult;
import com.neusoft.hospital.common.Result;
import com.neusoft.hospital.dto.response.PatientProfileResponse;
import com.neusoft.hospital.dto.response.PatientRecordDetailResponse;
import com.neusoft.hospital.dto.response.PatientRecordSummaryResponse;
import com.neusoft.hospital.service.PatientPortalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 患者门户接口（PR3）。
 * <p>
 * 类级 {@code @RequireRole(PATIENT)}：仅 PATIENT 可访问；ADMIN/DOCTOR 默认 403（角色平级，不继承）。
 * 患者身份一律由 Service 从 CurrentUser / 已验证 JWT 取得，Controller 不接收 patientId / employeeId。
 */
@Tag(name = "患者门户", description = "患者侧个人资料与就诊记录查询（仅 PATIENT）")
@RestController
@RequestMapping("/api/v1/patient")
@RequiredArgsConstructor
@RequireRole(Role.PATIENT)
public class PatientController {

    private final PatientPortalService patientPortalService;

    @Operation(summary = "患者个人资料", description = "返回当前登录患者的非敏感展示资料（不含身份证/手机号/家庭地址）",
            security = @SecurityRequirement(name = "Bearer"))
    @GetMapping("/profile")
    public Result<PatientProfileResponse> profile() {
        return Result.ok(patientPortalService.getCurrentPatientProfile());
    }

    @Operation(summary = "患者就诊记录列表", description = "分页查询当前患者的就诊记录摘要，仅返回 register.patient_id = 当前患者 的记录",
            security = @SecurityRequirement(name = "Bearer"))
    @GetMapping("/records")
    public Result<PageResult<PatientRecordSummaryResponse>> records(
            @Parameter(description = "页码，默认1", example = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数，默认10", example = "10") @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(patientPortalService.pageCurrentPatientRecords(pageNum, pageSize));
    }

    @Operation(summary = "患者就诊详情", description = "按挂号ID查询当前患者单次就诊详情（病历/处方/检查/检验/处置）；不属于当前患者的 registerId 返回 404",
            security = @SecurityRequirement(name = "Bearer"))
    @GetMapping("/records/{registerId}")
    public Result<PatientRecordDetailResponse> recordDetail(
            @Parameter(description = "挂号ID", example = "1", required = true) @PathVariable Integer registerId) {
        return Result.ok(patientPortalService.getCurrentPatientRecordDetail(registerId));
    }
}
