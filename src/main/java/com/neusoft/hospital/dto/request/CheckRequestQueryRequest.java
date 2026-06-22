package com.neusoft.hospital.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "检查申请查询请求")
public class CheckRequestQueryRequest {

    @Schema(description = "挂号ID", example = "1")
    private Integer registerId;

    @Schema(description = "检查状态", example = "待检查")
    private String checkState;

    @Schema(description = "创建时间起始", example = "2024-06-01T00:00:00")
    private LocalDateTime creationTimeStart;

    @Schema(description = "创建时间截止", example = "2024-06-30T23:59:59")
    private LocalDateTime creationTimeEnd;
}
