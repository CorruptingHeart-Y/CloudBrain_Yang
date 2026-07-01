package com.neusoft.hospital.ai.controller;

import com.neusoft.hospital.ai.dto.PrescriptionAuditRecordDTO;
import com.neusoft.hospital.ai.dto.PrescriptionAuditResultDTO;
import com.neusoft.hospital.ai.dto.PrescriptionCheckRequest;
import com.neusoft.hospital.ai.service.PrescriptionAuditService;
import com.neusoft.hospital.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "AI处方审核", description = "按挂号聚合处方明细，AI审核用药合理性/相互作用/风险")
@RestController
@RequestMapping("/api/v1/prescription/check")
@RequiredArgsConstructor
public class PrescriptionAuditController {

    private final PrescriptionAuditService prescriptionAuditService;

    @Operation(summary = "处方审核预览",
            description = "按挂号ID聚合处方明细交 AI 审核，返回风险等级/建议/相互作用/风险项；仅预览不落库。AI 不可用时返回 503 提示人工核对。",
            security = @SecurityRequirement(name = "Bearer"))
    @PostMapping
    public Result<PrescriptionAuditResultDTO> preview(@RequestBody @Valid PrescriptionCheckRequest request) {
        return Result.ok(prescriptionAuditService.audit(request.getRegisterId(), false));
    }

    @Operation(summary = "处方审核并落库",
            description = "同预览审核，并将请求快照与结果写入 prescription_audit_record，供复盘追溯；用于医生确认/保存处方时调用。",
            security = @SecurityRequirement(name = "Bearer"))
    @PostMapping("/confirm")
    public Result<PrescriptionAuditResultDTO> confirm(@RequestBody @Valid PrescriptionCheckRequest request) {
        return Result.ok(prescriptionAuditService.audit(request.getRegisterId(), true));
    }

    @Operation(summary = "查询处方审核记录",
            description = "按挂号ID查询其处方审核留痕记录，按创建时间倒序；requestSnapshot/resultJson 为 JSON 字符串，前端按需解析。",
            security = @SecurityRequirement(name = "Bearer"))
    @GetMapping("/record")
    public Result<List<PrescriptionAuditRecordDTO>> listByRegisterId(
            @Parameter(description = "挂号ID", example = "14", required = true) @RequestParam Integer registerId) {
        return Result.ok(prescriptionAuditService.listByRegisterId(registerId));
    }
}
