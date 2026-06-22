package com.neusoft.hospital.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "处置申请查询请求")
public class DisposalRequestQueryRequest {

    @Schema(description = "挂号ID", example = "1")
    private Integer registerId;

    @Schema(description = "处置状态", example = "待处置")
    private String disposalState;

    @Schema(description = "创建时间起始", example = "2024-06-01T00:00:00")
    private LocalDateTime creationTimeStart;

    @Schema(description = "创建时间截止", example = "2024-06-30T23:59:59")
    private LocalDateTime creationTimeEnd;
}
