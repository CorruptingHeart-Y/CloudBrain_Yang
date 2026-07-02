package com.neusoft.hospital.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 抢号票据：贯穿异步链路的状态机（PENDING→SUCCESS/FAILED）。
 * <p>幂等：uk_patient_date_noon 保证一患者每半日仅一号。
 */
@Data
@TableName("registration_ticket")
@Schema(description = "抢号票据")
public class RegistrationTicket {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID", example = "1")
    private Integer id;

    @Schema(description = "票据号", example = "QH2026070309301247")
    private String ticketNo;

    @Schema(description = "患者ID", example = "1")
    private Integer patientId;

    @Schema(description = "医生ID", example = "1")
    private Integer employeeId;

    @Schema(description = "就诊日期", example = "2026-07-03")
    private LocalDate visitDate;

    @Schema(description = "午别：上午/下午", example = "上午")
    private String noon;

    @Schema(description = "挂号级别ID", example = "1")
    private Integer registLevelId;

    @Schema(description = "结算类别ID", example = "1")
    private Integer settleCategoryId;

    @Schema(description = "状态：PENDING/SUCCESS/FAILED/CANCELLED", example = "PENDING")
    private String status;

    @Schema(description = "成功后回填挂号ID", example = "11")
    private Integer registerId;

    @Schema(description = "失败原因", example = "已约满")
    private String failReason;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
