package com.neusoft.hospital.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "挂号级别创建请求")
public class RegistLevelCreateRequest {

    @NotBlank(message = "挂号级别编码不能为空")
    @Schema(description = "挂号级别编码", example = "PT", requiredMode = Schema.RequiredMode.REQUIRED)
    private String registCode;

    @NotBlank(message = "挂号级别名称不能为空")
    @Schema(description = "挂号级别名称", example = "普通号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String registName;

    @Schema(description = "挂号费用", example = "14.00")
    private BigDecimal registFee;

    @Schema(description = "挂号限额", example = "100")
    private Integer registQuota;

    @Schema(description = "排序号", example = "1")
    private Integer sequenceNo;
}
