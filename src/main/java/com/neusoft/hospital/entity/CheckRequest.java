package com.neusoft.hospital.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 检查申请表
 */
@Data
@TableName("check_request")
@Schema(description = "检查申请表")
public class CheckRequest {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID", example = "1")
    private Integer id;

    @Schema(description = "挂号ID", example = "1")
    private Integer registerId;

    @Schema(description = "医技项目ID", example = "3")
    private Integer medicalTechnologyId;

    @Schema(description = "检查信息", example = "胸部CT检查")
    private String checkInfo;

    @Schema(description = "检查部位", example = "胸部")
    private String checkPosition;

    @Schema(description = "创建时间", example = "2026-06-01T10:00:00")
    private LocalDateTime creationTime;

    @Schema(description = "检查医生ID", example = "5")
    private Integer checkEmployeeId;

    @Schema(description = "录入检查医生ID", example = "6")
    private Integer inputcheckEmployeeId;

    @Schema(description = "检查时间", example = "2026-06-01T14:30:00")
    private LocalDateTime checkTime;

    @Schema(description = "检查结果", example = "未见明显异常")
    private String checkResult;

    @Schema(description = "检查状态(0-未检,1-已检)", example = "0")
    private String checkState;

    @Schema(description = "检查备注", example = "患者配合良好")
    private String checkRemark;
}
