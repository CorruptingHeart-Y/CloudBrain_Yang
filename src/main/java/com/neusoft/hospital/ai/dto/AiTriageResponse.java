package com.neusoft.hospital.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * Python /ai/triage 返回体，字段 camelCase 与 Python Pydantic alias 对齐。
 * 仅含 ID + reason + score，名称由 Spring Boot 侧用候选集富化。
 */
@Data
@Schema(description = "AI分诊响应(来自Python)")
public class AiTriageResponse {

    @Schema(description = "推荐科室列表")
    private List<RecommendedDept> departments;

    @Schema(description = "推荐医生列表")
    private List<RecommendedDoctor> doctors;

    @Data
    @Schema(description = "推荐科室")
    public static class RecommendedDept {
        private Integer deptId;
        private String reason;
        private Double score;
    }

    @Data
    @Schema(description = "推荐医生")
    public static class RecommendedDoctor {
        private Integer employeeId;
        private Integer deptId;
        private String reason;
        private Double score;
    }
}
