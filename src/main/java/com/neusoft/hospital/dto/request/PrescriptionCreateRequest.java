package com.neusoft.hospital.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "处方创建请求")
public class PrescriptionCreateRequest {

    @NotNull(message = "挂号ID不能为空")
    @Schema(description = "挂号ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer registerId;

    @NotNull(message = "药品ID不能为空")
    @Schema(description = "药品ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer drugId;

    @Schema(description = "药品用法", example = "口服，一日三次，一次1片")
    private String drugUsage;

    @Schema(description = "药品数量", example = "30")
    private Integer drugNumber;
}
