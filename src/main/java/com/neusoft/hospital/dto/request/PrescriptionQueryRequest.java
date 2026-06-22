package com.neusoft.hospital.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "处方查询请求")
public class PrescriptionQueryRequest {

    @Schema(description = "挂号ID", example = "1")
    private Integer registerId;

    @Schema(description = "药品状态", example = "待发药")
    private String drugState;
}
