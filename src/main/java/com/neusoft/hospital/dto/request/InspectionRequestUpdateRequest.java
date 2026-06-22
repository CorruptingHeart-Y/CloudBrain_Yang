package com.neusoft.hospital.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "检验申请更新请求")
public class InspectionRequestUpdateRequest {

    @NotNull(message = "ID不能为空")
    @Schema(description = "检验申请ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer id;

    @Schema(description = "检验医生ID", example = "3")
    private Integer inspectionEmployeeId;

    @Schema(description = "检验时间", example = "2024-06-15T11:00:00")
    private LocalDateTime inspectionTime;

    @Schema(description = "检验结果", example = "各项指标正常")
    private String inspectionResult;

    @Schema(description = "检验状态", example = "已完成")
    private String inspectionState;

    @Schema(description = "检验备注")
    private String inspectionRemark;
}
