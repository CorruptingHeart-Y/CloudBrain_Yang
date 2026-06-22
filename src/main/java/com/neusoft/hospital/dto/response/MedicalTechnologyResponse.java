package com.neusoft.hospital.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "医技项目响应DTO")
public class MedicalTechnologyResponse {

    @Schema(description = "医技项目ID", example = "1")
    private Integer id;

    @Schema(description = "医技项目编码", example = "JCH001")
    private String techCode;

    @Schema(description = "医技项目名称", example = "头部CT")
    private String techName;

    @Schema(description = "医技项目规格", example = "次")
    private String techFormat;

    @Schema(description = "医技项目价格", example = "280.00")
    private BigDecimal techPrice;

    @Schema(description = "医技项目类型", example = "检查")
    private String techType;

    @Schema(description = "价格类型", example = "医疗服务")
    private String priceType;

    @Schema(description = "执行科室ID", example = "1")
    private Integer deptmentId;

    @Schema(description = "执行科室名称(冗余)", example = "影像科")
    private String deptName;
}
