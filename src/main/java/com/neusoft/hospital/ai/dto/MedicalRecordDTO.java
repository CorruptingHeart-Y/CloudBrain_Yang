package com.neusoft.hospital.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 病历响应（落库后或查询返前端，含关联疾病）。
 */
@Data
@Schema(description = "病历响应")
public class MedicalRecordDTO {

    @Schema(description = "病历主键ID", example = "1")
    private Integer id;

    @Schema(description = "挂号ID", example = "14")
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

    @Schema(description = "关联疾病列表")
    private List<DiseaseSimpleDTO> diseases;

    @Data
    @Schema(description = "疾病简要信息")
    public static class DiseaseSimpleDTO {
        private Integer id;
        private String diseaseName;
        private String diseaseICD;
    }
}
