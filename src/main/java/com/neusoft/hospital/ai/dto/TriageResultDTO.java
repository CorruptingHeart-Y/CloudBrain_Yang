package com.neusoft.hospital.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 返回前端的分诊结果：在 AI 返回的纯 ID 基础上富化科室名/医生名/挂号级别名，前端可直接展示。
 */
@Data
@Schema(description = "分诊结果")
public class TriageResultDTO {

    @Schema(description = "推荐科室列表(按推荐度降序)")
    private List<RecommendedDeptDTO> departments;

    @Schema(description = "推荐医生列表(按推荐度降序)")
    private List<RecommendedDoctorDTO> doctors;

    @Data
    @Schema(description = "推荐科室")
    public static class RecommendedDeptDTO {
        private Integer deptId;
        private String deptName;
        private String reason;
        private Double score;
    }

    @Data
    @Schema(description = "推荐医生")
    public static class RecommendedDoctorDTO {
        private Integer employeeId;
        private String realname;
        private Integer deptId;
        private String deptName;
        private String registLevelName;
        private String reason;
        private Double score;
    }
}
