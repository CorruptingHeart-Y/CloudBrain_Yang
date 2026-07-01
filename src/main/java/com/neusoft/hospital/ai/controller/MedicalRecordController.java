package com.neusoft.hospital.ai.controller;

import com.neusoft.hospital.ai.dto.MedicalRecordDraftDTO;
import com.neusoft.hospital.ai.dto.MedicalRecordDTO;
import com.neusoft.hospital.ai.dto.MedicalRecordGenerateRequest;
import com.neusoft.hospital.ai.dto.MedicalRecordSaveRequest;
import com.neusoft.hospital.ai.service.MedicalRecordService;
import com.neusoft.hospital.auth.annotation.RequireRole;
import com.neusoft.hospital.auth.enums.Role;
import com.neusoft.hospital.common.Result;
import com.neusoft.hospital.service.RegisterOwnership;
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

@Tag(name = "AI病历生成", description = "医患对话文本→AI生成病历草稿，医生确认后落库并关联ICD疾病")
@RestController
@RequestMapping("/api/v1/medical-record")
@RequiredArgsConstructor
@RequireRole({Role.ADMIN, Role.DOCTOR})
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;
    private final RegisterOwnership registerOwnership;

    @Operation(summary = "生成病历草稿",
            description = "根据挂号ID + 医患对话文本，AI 生成 9 字段病历草稿，仅预览不落库。AI 不可用时返回 503 提示人工书写。",
            security = @SecurityRequirement(name = "Bearer"))
    @PostMapping("/generate")
    public Result<MedicalRecordDraftDTO> generate(@RequestBody @Valid MedicalRecordGenerateRequest request) {
        registerOwnership.requireAccessibleRegister(request.getRegisterId());
        return Result.ok(medicalRecordService.generate(request.getRegisterId(), request.getDialogue()));
    }

    @Operation(summary = "保存/确认病历",
            description = "将医生确认（可能编辑过）的病历落 medical_record（按挂号 upsert）+ medical_record_meta 新表记录来源，并替换 medical_record_disease 疾病关联。source=A 表示内容由 AI 草稿生成。",
            security = @SecurityRequirement(name = "Bearer"))
    @PostMapping
    public Result<MedicalRecordDTO> save(@RequestBody @Valid MedicalRecordSaveRequest request) {
        registerOwnership.requireAccessibleRegister(request.getRegisterId());
        return Result.ok(medicalRecordService.save(request));
    }

    @Operation(summary = "查询病历",
            description = "按挂号ID查询已落库病历（含关联 ICD 疾病列表）；该挂号未落库则 data 为 null。",
            security = @SecurityRequirement(name = "Bearer"))
    @GetMapping
    public Result<MedicalRecordDTO> getByRegisterId(
            @Parameter(description = "挂号ID", example = "14", required = true) @RequestParam Integer registerId) {
        return Result.ok(medicalRecordService.getByRegisterId(registerId));
    }
}
