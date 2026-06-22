package com.neusoft.hospital.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "挂号级别更新请求")
public class RegistLevelUpdateRequest {

    @NotNull(message = "ID不能为空")
    @Schema(description = "挂号级别ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer id;

    @Schema(description = "挂号级别编码", example = "PT")
    private String registCode;

    @Schema(description = "挂号级别名称", example = "普通号")
    private String registName;

    @Schema(description = "挂号费用", example = "14.00")
    private BigDecimal registFee;

    @Schema(description = "挂号限额", example = "100")
    private Integer registQuota;

    @Schema(description = "排序号", example = "1")
    private Integer sequenceNo;
}
