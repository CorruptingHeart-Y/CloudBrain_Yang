package com.neusoft.hospital.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "医技项目创建请求")
public class MedicalTechnologyCreateRequest {

    @NotBlank(message = "项目编码不能为空")
    @Schema(description = "项目编码", example = "JCH001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String techCode;

    @NotBlank(message = "项目名称不能为空")
    @Schema(description = "项目名称", example = "头部CT", requiredMode = Schema.RequiredMode.REQUIRED)
    private String techName;

    @Schema(description = "项目规格", example = "次")
    private String techFormat;

    @Schema(description = "项目价格", example = "280.00")
    private BigDecimal techPrice;

    @Schema(description = "项目类型", example = "检查")
    private String techType;

    @Schema(description = "价格类型", example = "医疗服务")
    private String priceType;

    @Schema(description = "科室ID", example = "1")
    private Integer deptmentId;
}
