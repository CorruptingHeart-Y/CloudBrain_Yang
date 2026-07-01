package com.neusoft.hospital.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 前端入参：按挂号ID审核该挂号下全部处方明细。
 */
@Data
@Schema(description = "处方审核请求(前端入参)")
public class PrescriptionCheckRequest {

    @Schema(description = "挂号ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "挂号ID不能为空")
    private Integer registerId;
}
