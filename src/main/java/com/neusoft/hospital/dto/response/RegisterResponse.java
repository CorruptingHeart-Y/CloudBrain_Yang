package com.neusoft.hospital.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "挂号响应DTO")
public class RegisterResponse {

    @Schema(description = "挂号ID", example = "1")
    private Integer id;

    @Schema(description = "病历号", example = "BL20240001")
    private String caseNumber;

    @Schema(description = "真实姓名", example = "张三")
    private String realName;

    @Schema(description = "性别", example = "男")
    private String gender;

    @Schema(description = "身份证号", example = "210102199001011234")
    private String cardNumber;

    @Schema(description = "出生日期", example = "1990-01-01")
    private LocalDate birthdate;

    @Schema(description = "年龄", example = "34")
    private Integer age;

    @Schema(description = "年龄类型", example = "年")
    private String ageType;

    @Schema(description = "家庭住址", example = "沈阳市和平区")
    private String homeAddress;

    @Schema(description = "看诊日期", example = "2024-06-15T09:00:00")
    private LocalDateTime visitDate;

    @Schema(description = "午别", example = "上午")
    private String noon;

    @Schema(description = "科室ID", example = "1")
    private Integer deptmentId;

    @Schema(description = "科室名称(冗余)", example = "神经内科")
    private String deptName;

    @Schema(description = "医生ID", example = "5")
    private Integer employeeId;

    @Schema(description = "医生姓名(冗余)", example = "李医生")
    private String employeeName;

    @Schema(description = "挂号级别ID", example = "1")
    private Integer registLevelId;

    @Schema(description = "挂号级别名称(冗余)", example = "普通号")
    private String registLevelName;

    @Schema(description = "结算类别ID", example = "2")
    private Integer settleCategoryId;

    @Schema(description = "结算类别名称(冗余)", example = "医保")
    private String settleCategoryName;

    @Schema(description = "是否预约", example = "否")
    private String isBook;

    @Schema(description = "挂号方式", example = "医保卡")
    private String registMethod;

    @Schema(description = "挂号费用", example = "14.00")
    private BigDecimal registMoney;

    @Schema(description = "看诊状态：1-已挂号 2-医生接诊 3-看诊结束 4-已退号", example = "1")
    private Integer visitState;
}
