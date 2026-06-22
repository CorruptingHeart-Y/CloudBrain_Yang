package com.neusoft.hospital.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "检验申请响应DTO")
public class InspectionRequestResponse {

    @Schema(description = "检验申请ID", example = "1")
    private Integer id;

    @Schema(description = "挂号ID", example = "1")
    private Integer registerId;

    @Schema(description = "医技项目ID", example = "2")
    private Integer medicalTechnologyId;

    @Schema(description = "医技项目名称(冗余)", example = "血常规")
    private String techName;

    @Schema(description = "检验信息", example = "血常规检查")
    private String inspectionInfo;

    @Schema(description = "检验部位", example = "静脉血")
    private String inspectionPosition;

    @Schema(description = "创建时间", example = "2024-06-15T09:30:00")
    private LocalDateTime creationTime;

    @Schema(description = "检验员工ID", example = "3")
    private Integer inspectionEmployeeId;

    @Schema(description = "检验时间", example = "2024-06-15T11:00:00")
    private LocalDateTime inspectionTime;

    @Schema(description = "检验结果", example = "各项指标正常")
    private String inspectionResult;

    @Schema(description = "检验状态", example = "已完成")
    private String inspectionState;

    @Schema(description = "检验备注", example = "")
    private String inspectionRemark;
}
