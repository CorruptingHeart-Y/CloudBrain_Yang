package com.neusoft.hospital.controller;

import com.neusoft.hospital.auth.annotation.RequireRole;
import com.neusoft.hospital.auth.enums.Role;
import com.neusoft.hospital.common.Result;
import com.neusoft.hospital.dto.request.QuotaCreateRequest;
import com.neusoft.hospital.dto.response.QuotaResponse;
import com.neusoft.hospital.entity.DoctorDailyQuota;
import com.neusoft.hospital.service.DoctorDailyQuotaService;
import com.neusoft.hospital.service.QuotaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 放号管理（ADMIN）。
 * <p>按医生+日期+午别放号；放号/重置后同步灌入 Redis 库存。
 */
@Tag(name = "号源管理", description = "ADMIN 按医生/日期/午别放号；抢号前必须先放号")
@RestController
@RequestMapping("/api/v1/register/quota")
@RequiredArgsConstructor
@RequireRole(Role.ADMIN)
@SecurityRequirement(name = "Bearer")
public class RegistrationQuotaController {

    private final DoctorDailyQuotaService doctorDailyQuotaService;
    private final QuotaService quotaService;

    @Operation(summary = "放号/重置号源", description = "upsert：存在则重置 remaining=capacity，否则新建；同步灌入 Redis")
    @PostMapping
    public Result<QuotaResponse> create(@RequestBody @Valid QuotaCreateRequest request) {
        DoctorDailyQuota q = doctorDailyQuotaService.upsert(
                request.getEmployeeId(), request.getQuotaDate(), request.getNoon(), request.getCapacity());
        quotaService.seedRedis(request.getEmployeeId(), request.getQuotaDate(), request.getNoon());
        return Result.ok(toResponse(q));
    }

    @Operation(summary = "查询号源", description = "按医生+日期查询所有午别号源（含剩余）")
    @GetMapping
    public Result<List<QuotaResponse>> list(
            @Parameter(description = "医生ID", required = true) @RequestParam Integer employeeId,
            @Parameter(description = "日期 yyyy-MM-dd", required = true) @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<DoctorDailyQuota> list = doctorDailyQuotaService.listByEmpDate(employeeId, date);
        return Result.ok(list.stream().map(this::toResponse).toList());
    }

    private QuotaResponse toResponse(DoctorDailyQuota q) {
        QuotaResponse r = new QuotaResponse();
        BeanUtils.copyProperties(q, r);
        return r;
    }
}
