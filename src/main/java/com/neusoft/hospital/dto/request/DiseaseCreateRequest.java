package com.neusoft.hospital.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "疾病创建请求")
public class DiseaseCreateRequest {

    @NotBlank(message = "疾病编码不能为空")
    @Schema(description = "疾病编码", example = "SJNK", requiredMode = Schema.RequiredMode.REQUIRED)
    private String diseaseCode;

    @NotBlank(message = "疾病名称不能为空")
    @Schema(description = "疾病名称", example = "脑梗死", requiredMode = Schema.RequiredMode.REQUIRED)
    private String diseaseName;

    @Schema(description = "ICD编码", example = "I63.9")
    private String diseaseICD;

    @Schema(description = "疾病分类", example = "神经系统疾病")
    private String diseaseCategory;
}
