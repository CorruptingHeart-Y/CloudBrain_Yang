package com.neusoft.hospital.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("drug_info")
@Schema(description = "药品信息表")
public class DrugInfo {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID", example = "1")
    private Integer id;

    @Schema(description = "药品编码，如：YP001", example = "YP001")
    private String drugCode;

    @Schema(description = "药品名称，如：阿莫西林胶囊", example = "阿莫西林胶囊")
    private String drugName;

    @Schema(description = "药品规格，如：0.5g*24粒/盒", example = "0.5g*24粒/盒")
    private String drugFormat;

    @Schema(description = "药品单位，如：盒", example = "盒")
    private String drugUnit;

    @Schema(description = "生产厂家，如：华北制药", example = "华北制药")
    private String manufacturer;

    @Schema(description = "药品剂型，如：胶囊剂", example = "胶囊剂")
    private String drugDosage;

    @Schema(description = "药品类型，如：西药", example = "西药")
    private String drugType;

    @Schema(description = "药品价格，如：12.50", example = "12.50")
    private BigDecimal drugPrice;

    @Schema(description = "助记码，如：AMXL", example = "AMXL")
    private String mnemonicCode;

    @Schema(description = "创建日期，如：2024-01-01", example = "2024-01-01")
    private LocalDate creationDate;
}
