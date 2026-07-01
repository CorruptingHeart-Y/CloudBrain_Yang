package com.neusoft.hospital.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 诊前分诊前端请求。
 * chiefComplaint 必填；cardNumber/caseNumber/patientName 用于落 triage_record（挂号前可能尚无 register）。
 */
@Data
@Schema(description = "诊前分诊请求")
public class TriageConsultRequest {

    @NotBlank(message = "主诉不能为空")
    @Schema(description = "主诉", example = "头疼三天，伴有恶心", requiredMode = Schema.RequiredMode.REQUIRED)
    private String chiefComplaint;

    @Schema(description = "患者简要信息")
    private PatientBrief patient;

    @Schema(description = "患者身份证号（落记录用）", example = "210102199001011234")
    private String cardNumber;

    @Schema(description = "病历号（落记录用）", example = "BL20260601001")
    private String caseNumber;

    @Schema(description = "患者姓名（落记录用）", example = "李四")
    private String patientName;
}
