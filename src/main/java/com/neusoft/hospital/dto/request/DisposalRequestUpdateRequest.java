package com.neusoft.hospital.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "处置申请更新请求")
public class DisposalRequestUpdateRequest {

    @NotNull(message = "ID不能为空")
    @Schema(description = "处置申请ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer id;

    @Schema(description = "处置医生ID", example = "3")
    private Integer disposalEmployeeId;

    @Schema(description = "处置时间", example = "2024-06-15T14:00:00")
    private LocalDateTime disposalTime;

    @Schema(description = "处置结果", example = "处置完成")
    private String disposalResult;

    @Schema(description = "处置状态", example = "已完成")
    private String disposalState;

    @Schema(description = "处置备注")
    private String disposalRemark;
}
