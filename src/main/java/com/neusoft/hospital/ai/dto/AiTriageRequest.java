package com.neusoft.hospital.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 发往 Python /ai/triage 的请求体，字段 camelCase 与 Python Pydantic alias 对齐。
 */
@Data
@Schema(description = "AI分诊请求(发往Python)")
public class AiTriageRequest {

    @Schema(description = "主诉")
    private String chiefComplaint;

    @Schema(description = "患者简要信息")
    private PatientBrief patient;

    @Schema(description = "候选科室列表")
    private List<CandidateDept> departments;

    @Schema(description = "候选医生列表")
    private List<CandidateDoctor> doctors;

    @Data
    @Schema(description = "候选科室")
    public static class CandidateDept {
        private Integer deptId;
        private String deptName;
        private String deptType;
    }

    @Data
    @Schema(description = "候选医生")
    public static class CandidateDoctor {
        private Integer employeeId;
        private Integer deptId;
        private String realname;
        private String registLevelName;
    }
}
