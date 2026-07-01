package com.neusoft.hospital.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 处方审核结果（富化后返前端，含药名）。
 */
@Data
@Schema(description = "处方审核结果")
public class PrescriptionAuditResultDTO {

    @Schema(description = "总体风险等级：low/medium/high", example = "medium")
    private String riskLevel;

    @Schema(description = "用药建议列表")
    private List<SuggestionDTO> suggestions;

    @Schema(description = "药物相互作用列表")
    private List<InteractionDTO> interactions;

    @Schema(description = "风险项列表")
    private List<RiskItemDTO> riskItems;

    @Data
    @Schema(description = "用药建议")
    public static class SuggestionDTO {
        private Integer drugId;
        private String drugName;
        private String content;
    }

    @Data
    @Schema(description = "药物相互作用")
    public static class InteractionDTO {
        private Integer drugA;
        private String drugAName;
        private Integer drugB;
        private String drugBName;
        private String level;
        private String desc;
    }

    @Data
    @Schema(description = "风险项")
    public static class RiskItemDTO {
        private Integer drugId;
        private String drugName;
        private String type;
        private String desc;
    }
}
