package com.neusoft.hospital.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "处置申请响应DTO")
public class DisposalRequestResponse {

    @Schema(description = "处置申请ID", example = "1")
    private Integer id;

    @Schema(description = "挂号ID", example = "1")
    private Integer registerId;

    @Schema(description = "医技项目ID", example = "3")
    private Integer medicalTechnologyId;

    @Schema(description = "医技项目名称(冗余)", example = "换药处置")
    private String techName;

    @Schema(description = "处置信息", example = "换药处置")
    private String disposalInfo;

    @Schema(description = "处置部位", example = "右手")
    private String disposalPosition;

    @Schema(description = "创建时间", example = "2024-06-15T09:30:00")
    private LocalDateTime creationTime;

    @Schema(description = "处置员工ID", example = "3")
    private Integer disposalEmployeeId;

    @Schema(description = "处置时间", example = "2024-06-15T14:00:00")
    private LocalDateTime disposalTime;

    @Schema(description = "处置结果", example = "处置完成")
    private String disposalResult;

    @Schema(description = "处置状态", example = "已完成")
    private String disposalState;

    @Schema(description = "处置备注", example = "")
    private String disposalRemark;
}
