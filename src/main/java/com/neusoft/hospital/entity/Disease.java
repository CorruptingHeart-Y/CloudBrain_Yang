package com.neusoft.hospital.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@TableName("disease")
@Schema(description = "疾病表")
public class Disease {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID", example = "1")
    private Integer id;

    @Schema(description = "疾病编码，如：JB001", example = "JB001")
    private String diseaseCode;

    @Schema(description = "疾病名称，如：感冒", example = "感冒")
    private String diseaseName;

    @TableField("diseaseICD")
    @Schema(description = "疾病ICD编码，如：J00", example = "J00")
    private String diseaseICD;

    @Schema(description = "疾病分类，如：呼吸系统疾病", example = "呼吸系统疾病")
    private String diseaseCategory;
}
