package com.neusoft.hospital.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 患者主索引表
 */
@Data
@TableName("patient")
@Schema(description = "患者主索引表")
public class Patient {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID", example = "1")
    private Integer id;

    @Schema(description = "姓名", example = "王建国")
    private String realName;

    @Schema(description = "性别：男/女", example = "男")
    private String gender;

    @Schema(description = "身份证号（患者唯一身份键）", example = "210102195503151234")
    private String cardNumber;

    @Schema(description = "出生日期", example = "1955-03-15")
    private LocalDate birthdate;

    @Schema(description = "手机号", example = "13800000000")
    private String phone;

    @Schema(description = "家庭住址", example = "沈阳市和平区中山路88号")
    private String homeAddress;

    @TableLogic
    @Schema(description = "删除标记：1-正常 0-已删除", example = "1")
    private Integer delmark;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
