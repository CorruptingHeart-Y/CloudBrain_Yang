package com.neusoft.hospital.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "结算类别创建请求")
public class SettleCategoryCreateRequest {

    @NotBlank(message = "结算类别编码不能为空")
    @Schema(description = "结算类别编码", example = "YB", requiredMode = Schema.RequiredMode.REQUIRED)
    private String settleCode;

    @NotBlank(message = "结算类别名称不能为空")
    @Schema(description = "结算类别名称", example = "医保", requiredMode = Schema.RequiredMode.REQUIRED)
    private String settleName;

    @Schema(description = "排序号", example = "1")
    private Integer sequenceNo;
}
