package com.neusoft.hospital.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 前端入参：根据挂号ID + 医患对话文本生成病历草稿（仅预览，不落库）。
 */
@Data
@Schema(description = "病历生成请求(前端入参)")
public class MedicalRecordGenerateRequest {

    @Schema(description = "挂号ID", example = "14", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "挂号ID不能为空")
    private Integer registerId;

    @Schema(description = "医患对话文本，医生手输/粘贴", example = "患者诉头疼三天...", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "对话文本不能为空")
    private String dialogue;
}
