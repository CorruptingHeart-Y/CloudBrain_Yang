package com.neusoft.hospital.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 处方表
 */
@Data
@TableName("prescription")
@Schema(description = "处方表")
public class Prescription {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID", example = "1")
    private Integer id;

    @Schema(description = "挂号ID", example = "1")
    private Integer registerId;

    @Schema(description = "药品ID", example = "100")
    private Integer drugId;

    @Schema(description = "药品用法", example = "口服，一日三次")
    private String drugUsage;

    @Schema(description = "药品数量", example = "30")
    private String drugNumber;

    @Schema(description = "创建时间", example = "2026-06-01T10:30:00")
    private LocalDateTime creationTime;

    @Schema(description = "药品状态(0-未取药,1-已取药)", example = "0")
    private String drugState;
}
