package com.neusoft.hospital.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "检查申请响应DTO")
public class CheckRequestResponse {

    @Schema(description = "检查申请ID", example = "1")
    private Integer id;

    @Schema(description = "挂号ID", example = "1")
    private Integer registerId;

    @Schema(description = "医技项目ID", example = "1")
    private Integer medicalTechnologyId;

    @Schema(description = "医技项目名称(冗余)", example = "头部CT")
    private String techName;

    @Schema(description = "检查信息", example = "头部CT检查")
    private String checkInfo;

    @Schema(description = "检查部位", example = "头部")
    private String checkPosition;

    @Schema(description = "创建时间", example = "2024-06-15T09:30:00")
    private LocalDateTime creationTime;

    @Schema(description = "检查员工ID", example = "3")
    private Integer checkEmployeeId;

    @Schema(description = "检查时间", example = "2024-06-15T10:30:00")
    private LocalDateTime checkTime;

    @Schema(description = "检查结果", example = "未见明显异常")
    private String checkResult;

    @Schema(description = "检查状态", example = "已完成")
    private String checkState;

    @Schema(description = "检查备注", example = "")
    private String checkRemark;
}
