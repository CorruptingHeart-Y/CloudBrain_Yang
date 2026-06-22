package com.neusoft.hospital.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "结算类别更新请求")
public class SettleCategoryUpdateRequest {

    @NotNull(message = "ID不能为空")
    @Schema(description = "结算类别ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer id;

    @Schema(description = "结算类别编码", example = "YB")
    private String settleCode;

    @Schema(description = "结算类别名称", example = "医保")
    private String settleName;

    @Schema(description = "排序号", example = "1")
    private Integer sequenceNo;
}
