package com.neusoft.hospital.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 患者历次挂号信息表
 */
@Data
@TableName("register")
@Schema(description = "患者历次挂号信息表")
public class Register {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID", example = "1")
    private Integer id;

    @Schema(description = "病历号", example = "BL20260601001")
    private String caseNumber;

    @Schema(description = "患者真实姓名", example = "李四")
    private String realName;

    @Schema(description = "性别", example = "男")
    private String gender;

    @Schema(description = "身份证号", example = "210102199001011234")
    private String cardNumber;

    @Schema(description = "出生日期", example = "1990-01-01")
    private LocalDate birthdate;

    @Schema(description = "年龄", example = "36")
    private Integer age;

    @Schema(description = "年龄类型(岁/月/天)", example = "岁")
    private String ageType;

    @Schema(description = "家庭住址", example = "北京市朝阳区")
    private String homeAddress;

    @Schema(description = "就诊日期时间", example = "2026-06-01T09:30:00")
    private LocalDateTime visitDate;

    @Schema(description = "上午/下午", example = "上午")
    private String noon;

    @Schema(description = "科室ID", example = "10")
    private Integer deptmentId;

    @Schema(description = "医生ID", example = "5")
    private Integer employeeId;

    @Schema(description = "挂号级别ID", example = "1")
    private Integer registLevelId;

    @Schema(description = "结算类别ID", example = "2")
    private Integer settleCategoryId;

    @Schema(description = "是否预约(0-否,1-是)", example = "0")
    private String isBook;

    @Schema(description = "挂号方式", example = "现场挂号")
    private String registMethod;

    @Schema(description = "挂号费用", example = "25.00")
    private BigDecimal registMoney;

    @Schema(description = "就诊状态(0-未诊,1-已诊,2-退号)", example = "0")
    private Integer visitState;
}
