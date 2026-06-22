package com.neusoft.hospital.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "处方更新请求")
public class PrescriptionUpdateRequest {

    @NotNull(message = "ID不能为空")
    @Schema(description = "处方ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer id;

    @Schema(description = "药品用法", example = "口服，一日三次，一次1片")
    private String drugUsage;

    @Schema(description = "药品数量", example = "30")
    private Integer drugNumber;

    @Schema(description = "药品状态", example = "已发药")
    private String drugState;
}
