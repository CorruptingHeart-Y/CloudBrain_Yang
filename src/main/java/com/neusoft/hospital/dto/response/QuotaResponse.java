package com.neusoft.hospital.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "号源信息")
public class QuotaResponse {

    @Schema(description = "号源ID", example = "1")
    private Integer id;

    @Schema(description = "医生ID", example = "1")
    private Integer employeeId;

    @Schema(description = "放号日期", example = "2026-07-03")
    private LocalDate quotaDate;

    @Schema(description = "午别", example = "上午")
    private String noon;

    @Schema(description = "总号源", example = "50")
    private Integer capacity;

    @Schema(description = "剩余号源", example = "47")
    private Integer remaining;
}
