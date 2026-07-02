package com.neusoft.hospital.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 医生每日号源表。
 * <p>ADMIN 按医生+日期+午别放号；remaining 为权威库存，Redis 仅作快速过滤缓存。
 */
@Data
@TableName("doctor_daily_quota")
@Schema(description = "医生每日号源")
public class DoctorDailyQuota {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID", example = "1")
    private Integer id;

    @Schema(description = "医生ID", example = "1")
    private Integer employeeId;

    @Schema(description = "放号日期", example = "2026-07-03")
    private LocalDate quotaDate;

    @Schema(description = "午别：上午/下午", example = "上午")
    private String noon;

    @Schema(description = "总号源", example = "50")
    private Integer capacity;

    @Schema(description = "剩余号源", example = "47")
    private Integer remaining;

    @TableLogic
    @Schema(description = "生效标记：1-正常 0-已删除", example = "1")
    private Integer delmark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
