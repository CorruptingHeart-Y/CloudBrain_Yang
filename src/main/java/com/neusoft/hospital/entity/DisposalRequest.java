package com.neusoft.hospital.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 处置申请表
 */
@Data
@TableName("disposal_request")
@Schema(description = "处置申请表")
public class DisposalRequest {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID", example = "1")
    private Integer id;

    @Schema(description = "挂号ID", example = "1")
    private Integer registerId;

    @Schema(description = "医技项目ID", example = "5")
    private Integer medicalTechnologyId;

    @Schema(description = "处置信息", example = "清创缝合术")
    private String disposalInfo;

    @Schema(description = "处置部位", example = "左手前臂")
    private String disposalPosition;

    @Schema(description = "创建时间", example = "2026-06-01T10:00:00")
    private LocalDateTime creationTime;

    @Schema(description = "处置医生ID", example = "9")
    private Integer disposalEmployeeId;

    @Schema(description = "录入处置医生ID", example = "10")
    private Integer inputdisposalEmployeeId;

    @Schema(description = "处置时间", example = "2026-06-01T16:00:00")
    private LocalDateTime disposalTime;

    @Schema(description = "处置结果", example = "缝合完成，恢复良好")
    private String disposalResult;

    @Schema(description = "处置状态(0-未处置,1-已处置)", example = "0")
    private String disposalState;

    @Schema(description = "处置备注", example = "术后注意换药")
    private String disposalRemark;
}
