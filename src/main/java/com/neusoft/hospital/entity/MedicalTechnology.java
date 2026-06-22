package com.neusoft.hospital.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("medical_technology")
@Schema(description = "诊疗项目表")
public class MedicalTechnology {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID", example = "1")
    private Integer id;

    @Schema(description = "项目编码，如：ZLXM001", example = "ZLXM001")
    private String techCode;

    @Schema(description = "项目名称，如：血常规", example = "血常规")
    private String techName;

    @Schema(description = "项目规格，如：次", example = "次")
    private String techFormat;

    @Schema(description = "项目价格，如：25.00", example = "25.00")
    private BigDecimal techPrice;

    @Schema(description = "项目类型，如：检查", example = "检查")
    private String techType;

    @Schema(description = "价格类型，如：自费", example = "自费")
    private String priceType;

    @Schema(description = "所属科室ID，如：1", example = "1")
    private Integer deptmentId;
}
