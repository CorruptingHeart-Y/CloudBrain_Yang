package com.neusoft.hospital.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * Python /ai/prescription-check 返回体，字段 camelCase 与 Python Pydantic alias 对齐。
 * 仅含 drugId，药名由 Spring Boot 侧用 drug_info 候选集富化。
 */
@Data
@Schema(description = "AI处方审核响应(来自Python)")
public class AiPrescriptionCheckResponse {

    @Schema(description = "总体风险等级：low/medium/high")
    private String riskLevel;

    @Schema(description = "用药建议列表")
    private List<Suggestion> suggestions;

    @Schema(description = "药物相互作用列表")
    private List<Interaction> interactions;

    @Schema(description = "风险项列表")
    private List<RiskItem> riskItems;

    @Data
    @Schema(description = "用药建议")
    public static class Suggestion {
        private Integer drugId;
        private String content;
    }

    @Data
    @Schema(description = "药物相互作用")
    public static class Interaction {
        private Integer drugA;
        private Integer drugB;
        private String level;
        private String desc;
    }

    @Data
    @Schema(description = "风险项")
    public static class RiskItem {
        private Integer drugId;
        private String type;
        private String desc;
    }
}
