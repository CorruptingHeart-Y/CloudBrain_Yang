package com.neusoft.hospital.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 前端入参：保存/确认病历（落 medical_record + 替换 medical_record_disease 关联）。
 * 由前端把 AI 草稿（可能经医生编辑）+ 医生从疾病字典选的 diseaseIds 一并提交。
 * source：A=AI草稿来源，M=人工录入；不传按 M 处理。
 */
@Data
@Schema(description = "病历保存请求(前端入参)")
public class MedicalRecordSaveRequest {

    @Schema(description = "挂号ID", example = "14", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "挂号ID不能为空")
    private Integer registerId;

    @Schema(description = "主诉")
    private String readme;

    @Schema(description = "现病史")
    private String present;

    @Schema(description = "现病治疗情况")
    private String presentTreat;

    @Schema(description = "既往史")
    private String history;

    @Schema(description = "过敏史")
    private String allergy;

    @Schema(description = "体格检查")
    private String physique;

    @Schema(description = "检查/检验建议")
    private String proposal;

    @Schema(description = "注意事项")
    private String careful;

    @Schema(description = "诊断结果")
    private String diagnosis;

    @Schema(description = "处理意见")
    private String cure;

    @Schema(description = "病历来源：A=AI草稿生成，M=人工录入", example = "A")
    private String source;

    @Schema(description = "关联疾病ID列表（医生从疾病字典选择）", example = "[3,7]")
    private List<Integer> diseaseIds;
}
