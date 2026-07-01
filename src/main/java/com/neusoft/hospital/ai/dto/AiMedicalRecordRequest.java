package com.neusoft.hospital.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 发往 Python /ai/medical-record 的请求体，字段 camelCase 与 Python Pydantic alias 对齐。
 */
@Data
@Schema(description = "AI病历生成请求(发往Python)")
public class AiMedicalRecordRequest {

    @Schema(description = "挂号ID")
    private Integer registerId;

    @Schema(description = "患者简要信息")
    private PatientBrief patient;

    @Schema(description = "医患对话文本，医生手输/粘贴")
    private String dialogue;
}
