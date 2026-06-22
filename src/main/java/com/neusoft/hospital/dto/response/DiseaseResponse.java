package com.neusoft.hospital.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "疾病响应DTO")
public class DiseaseResponse {

    @Schema(description = "疾病ID", example = "1")
    private Integer id;

    @Schema(description = "疾病编码", example = "SJNK")
    private String diseaseCode;

    @Schema(description = "疾病名称", example = "脑梗死")
    private String diseaseName;

    @Schema(description = "疾病ICD编码", example = "I63.9")
    private String diseaseICD;

    @Schema(description = "疾病分类", example = "神经系统疾病")
    private String diseaseCategory;
}
