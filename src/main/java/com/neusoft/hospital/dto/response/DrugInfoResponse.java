package com.neusoft.hospital.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "药品信息响应DTO")
public class DrugInfoResponse {

    @Schema(description = "药品ID", example = "1")
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

    @Schema(description = "创建日期", example = "2024-01-01")
    private LocalDate creationDate;
}
