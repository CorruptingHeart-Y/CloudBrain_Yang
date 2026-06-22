package com.neusoft.hospital.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "药品信息查询请求")
public class DrugInfoQueryRequest {

    @Schema(description = "关键词(匹配助记码或药品名称)", example = "ASPL")
    private String keyword;

    @Schema(description = "药品类型", example = "西药")
    private String drugType;
}
