package com.neusoft.hospital.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "结算类别响应DTO")
public class SettleCategoryResponse {

    @Schema(description = "结算类别ID", example = "1")
    private Integer id;

    @Schema(description = "结算类别编码", example = "YB")
    private String settleCode;

    @Schema(description = "结算类别名称", example = "医保")
    private String settleName;

    @Schema(description = "序号", example = "1")
    private Integer sequenceNo;
}
