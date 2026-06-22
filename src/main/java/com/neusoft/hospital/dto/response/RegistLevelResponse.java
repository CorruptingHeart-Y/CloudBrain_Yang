package com.neusoft.hospital.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "挂号级别响应DTO")
public class RegistLevelResponse {

    @Schema(description = "挂号级别ID", example = "1")
    private Integer id;

    @Schema(description = "挂号级别编码", example = "PT")
    private String registCode;

    @Schema(description = "挂号级别名称", example = "普通号")
    private String registName;

    @Schema(description = "挂号费用", example = "14.00")
    private BigDecimal registFee;

    @Schema(description = "挂号限额", example = "100")
    private Integer registQuota;

    @Schema(description = "序号", example = "1")
    private Integer sequenceNo;
}
