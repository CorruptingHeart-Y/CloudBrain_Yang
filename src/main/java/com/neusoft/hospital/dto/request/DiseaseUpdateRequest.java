package com.neusoft.hospital.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "疾病更新请求")
public class DiseaseUpdateRequest {

    @NotNull(message = "ID不能为空")
    @Schema(description = "疾病ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer id;

    @Schema(description = "疾病编码", example = "SJNK")
    private String diseaseCode;

    @Schema(description = "疾病名称", example = "脑梗死")
    private String diseaseName;

    @Schema(description = "ICD编码", example = "I63.9")
    private String diseaseICD;

    @Schema(description = "疾病分类", example = "神经系统疾病")
    private String diseaseCategory;
}
