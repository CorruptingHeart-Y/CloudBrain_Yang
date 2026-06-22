package com.neusoft.hospital.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 检验申请表
 */
@Data
@TableName("inspection_request")
@Schema(description = "检验申请表")
public class InspectionRequest {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID", example = "1")
    private Integer id;

    @Schema(description = "挂号ID", example = "1")
    private Integer registerId;

    @Schema(description = "医技项目ID", example = "4")
    private Integer medicalTechnologyId;

    @Schema(description = "检验信息", example = "血常规检验")
    private String inspectionInfo;

    @Schema(description = "检验部位", example = "静脉血")
    private String inspectionPosition;

    @Schema(description = "创建时间", example = "2026-06-01T10:00:00")
    private LocalDateTime creationTime;

    @Schema(description = "检验医生ID", example = "7")
    private Integer inspectionEmployeeId;

    @Schema(description = "录入检验医生ID", example = "8")
    private Integer inputinspectionEmployeeId;

    @Schema(description = "检验时间", example = "2026-06-01T15:00:00")
    private LocalDateTime inspectionTime;

    @Schema(description = "检验结果", example = "各项指标正常")
    private String inspectionResult;

    @Schema(description = "检验状态(0-未检,1-已检)", example = "0")
    private String inspectionState;

    @Schema(description = "检验备注", example = "空腹采血")
    private String inspectionRemark;
}
