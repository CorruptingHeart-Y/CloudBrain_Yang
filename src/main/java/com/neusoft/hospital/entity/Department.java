package com.neusoft.hospital.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@TableName("department")
@Schema(description = "科室表")
public class Department {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID", example = "1")
    private Integer id;

    @Schema(description = "科室编码，如：SJNK", example = "SJNK")
    private String deptCode;

    @Schema(description = "科室名称，如：神经内科", example = "神经内科")
    private String deptName;

    @Schema(description = "科室类型，如：临床", example = "临床")
    private String deptType;

    @TableLogic
    @Schema(description = "删除标记，0-未删除，1-已删除", example = "0")
    private Integer delmark;
}
