package com.neusoft.hospital.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "处方响应DTO")
public class PrescriptionResponse {

    @Schema(description = "处方ID", example = "1")
    private Integer id;

    @Schema(description = "挂号ID", example = "1")
    private Integer registerId;

    @Schema(description = "药品ID", example = "1")
    private Integer drugId;

    @Schema(description = "药品名称(冗余)", example = "阿司匹林肠溶片")
    private String drugName;

    @Schema(description = "药品规格(冗余)", example = "100mg*30片")
    private String drugFormat;

    @Schema(description = "药品用法", example = "口服，一日三次，一次1片")
    private String drugUsage;

    @Schema(description = "药品数量", example = "30")
    private String drugNumber;

    @Schema(description = "创建时间", example = "2024-06-15T10:00:00")
    private LocalDateTime creationTime;

    @Schema(description = "药品状态", example = "待发药")
    private String drugState;
}
