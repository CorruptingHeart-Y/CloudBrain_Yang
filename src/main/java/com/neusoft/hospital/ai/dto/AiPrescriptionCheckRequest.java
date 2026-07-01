package com.neusoft.hospital.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 发往 Python /ai/prescription-check 的请求体，字段 camelCase 与 Python Pydantic alias 对齐。
 */
@Data
@Schema(description = "AI处方审核请求(发往Python)")
public class AiPrescriptionCheckRequest {

    @Schema(description = "挂号ID")
    private Integer registerId;

    @Schema(description = "患者简要信息")
    private PatientBrief patient;

    @Schema(description = "处方药品列表")
    private List<DrugItem> drugs;

    @Data
    @Schema(description = "处方药品")
    public static class DrugItem {
        private Integer drugId;
        private String drugName;
        private String drugFormat;
        private String drugUsage;
        private String drugNumber;
    }
}
