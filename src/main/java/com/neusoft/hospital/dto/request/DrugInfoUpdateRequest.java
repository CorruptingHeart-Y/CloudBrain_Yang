package com.neusoft.hospital.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "药品信息更新请求")
public class DrugInfoUpdateRequest {

    @NotNull(message = "ID不能为空")
    @Schema(description = "药品ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer id;

    @Schema(description = "药品编码", example = "YP001")
    private String drugCode;

    @Schema(description = "药品名称", example = "阿司匹林肠溶片")
    private String drugName;

    @Schema(description = "药品规格", example = "100mg*30片")
    private String drugFormat;

    @Schema(description = "药品单位", example = "盒")
    private String drugUnit;

    @Schema(description = "生产厂家", example = "拜耳")
    private String manufacturer;

    @Schema(description = "药品剂型", example = "片剂")
    private String drugDosage;

    @Schema(description = "药品类型", example = "西药")
    private String drugType;

    @Schema(description = "药品价格", example = "25.50")
    private BigDecimal drugPrice;

    @Schema(description = "助记码", example = "ASPL")
    private String mnemonicCode;
}
