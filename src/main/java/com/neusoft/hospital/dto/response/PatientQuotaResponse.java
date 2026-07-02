package com.neusoft.hospital.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 患者可见的号源列表条目。
 * <p>联表 doctor_daily_quota + employee + department + regist_level，
 * 仅返回 remaining>0 的可抢号源，供患者抢号页浏览选择。
 */
@Data
@Schema(description = "患者号源列表条目")
public class PatientQuotaResponse {

    @Schema(description = "医生ID", example = "1")
    private Integer employeeId;

    @Schema(description = "医生姓名", example = "张伟")
    private String employeeName;

    @Schema(description = "科室ID", example = "1")
    private Integer deptmentId;

    @Schema(description = "科室名称", example = "神经内科")
    private String deptName;

    @Schema(description = "挂号级别ID（抢号时回传）", example = "2")
    private Integer registLevelId;

    @Schema(description = "挂号级别名称", example = "专家号")
    private String registLevelName;

    @Schema(description = "挂号费用（预览）", example = "50.00")
    private BigDecimal registFee;

    @Schema(description = "放号日期", example = "2026-07-03")
    private LocalDate quotaDate;

    @Schema(description = "午别：上午/下午", example = "上午")
    private String noon;

    @Schema(description = "总号源", example = "20")
    private Integer capacity;

    @Schema(description = "剩余号源", example = "5")
    private Integer remaining;
}
