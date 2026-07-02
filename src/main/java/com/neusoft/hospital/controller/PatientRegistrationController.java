package com.neusoft.hospital.controller;

import com.neusoft.hospital.auth.annotation.RequireRole;
import com.neusoft.hospital.auth.enums.Role;
import com.neusoft.hospital.common.Result;
import com.neusoft.hospital.dto.request.RegistrationGrabRequest;
import com.neusoft.hospital.dto.response.PatientQuotaResponse;
import com.neusoft.hospital.dto.response.RegistrationGrabResponse;
import com.neusoft.hospital.dto.response.RegistrationTicketResponse;
import com.neusoft.hospital.service.DoctorDailyQuotaService;
import com.neusoft.hospital.service.RegistrationGrabService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 患者抢号（秒杀）接口。
 * <p>仅 PATIENT：异步链路 = Redis Lua 扣减 + MQ 削峰 + 消费端 MySQL 兜底落库。
 * 患者身份一律由 Service 从 CurrentUser 取，Controller 不收 patientId。
 */
@Tag(name = "患者抢号", description = "患者自助网上抢号（异步削峰）；仅 PATIENT")
@RestController
@RequestMapping("/api/v1/patient/register")
@RequiredArgsConstructor
@RequireRole(Role.PATIENT)
@SecurityRequirement(name = "Bearer")
public class PatientRegistrationController {

    private final RegistrationGrabService registrationGrabService;
    private final DoctorDailyQuotaService doctorDailyQuotaService;

    @Operation(summary = "号源列表(只读)", description = "返回 [今天, 今天+days] 区间内 remaining>0 的可抢号源，含医生姓名/科室/级别名称/费用。days 钳制到 1~30，默认 7")
    @GetMapping("/quota")
    public Result<List<PatientQuotaResponse>> listQuota(
            @Parameter(description = "查询未来天数，默认7，范围1~30", example = "7")
            @RequestParam(defaultValue = "7") Integer days) {
        return Result.ok(doctorDailyQuotaService.listAvailableForPatient(days));
    }

    @Operation(summary = "抢号", description = "提交后立即返回 PENDING 票据；MQ 异步落库。一人一号(每半日)，重复抢号幂等返回既有票据")
    @PostMapping("/grab")
    public Result<RegistrationGrabResponse> grab(@RequestBody @Valid RegistrationGrabRequest request) {
        return Result.ok(registrationGrabService.grab(request));
    }

    @Operation(summary = "查询抢号结果", description = "按票据号轮询；SUCCESS 返回 registerId/caseNumber，FAILED 返回 failReason；非本人票据 → 404")
    @GetMapping("/grab/{ticketNo}")
    public Result<RegistrationTicketResponse> getResult(
            @Parameter(description = "票据号", required = true) @PathVariable String ticketNo) {
        return Result.ok(registrationGrabService.getResult(ticketNo));
    }
}
