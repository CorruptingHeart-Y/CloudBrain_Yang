package com.neusoft.hospital.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "检查申请更新请求")
public class CheckRequestUpdateRequest {

    @NotNull(message = "ID不能为空")
    @Schema(description = "检查申请ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer id;

    @Schema(description = "检查医生ID", example = "3")
    private Integer checkEmployeeId;

    @Schema(description = "检查时间", example = "2024-06-15T10:30:00")
    private LocalDateTime checkTime;

    @Schema(description = "检查结果", example = "未见明显异常")
    private String checkResult;

    @Schema(description = "检查状态", example = "已完成")
    private String checkState;

    @Schema(description = "检查备注")
    private String checkRemark;
}
