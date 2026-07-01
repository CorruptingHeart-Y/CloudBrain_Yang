package com.neusoft.hospital.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 患者↔挂号 桥接表（v2.0）。
 * <p>
 * 不在 register 上加 patient_id 列；患者归属经此表表达。
 * 一个 register 最多绑定一个 patient（uk_register_id）。
 */
@Data
@TableName("patient_register_link")
@Schema(description = "患者挂号关联桥接表")
public class PatientRegisterLink {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID", example = "1")
    private Integer id;

    @Schema(description = "患者id，指向patient(id)", example = "1")
    private Integer patientId;

    @Schema(description = "挂号id，指向register(id)", example = "1")
    private Integer registerId;

    @Schema(description = "关联来源：AUTO_INIT/AUTO_CREATE/MANUAL", example = "AUTO_INIT")
    private String linkSource;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
