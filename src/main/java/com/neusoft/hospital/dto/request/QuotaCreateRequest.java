package com.neusoft.hospital.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "放号请求")
public class QuotaCreateRequest {

    @NotNull(message = "医生ID不能为空")
    @Schema(description = "医生ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer employeeId;

    @NotNull(message = "放号日期不能为空")
    @Schema(description = "放号日期", example = "2026-07-03", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate quotaDate;

    @NotBlank(message = "午别不能为空")
    @Schema(description = "午别：上午/下午", example = "上午", requiredMode = Schema.RequiredMode.REQUIRED)
    private String noon;

    @NotNull(message = "总号源不能为空")
    @Min(value = 1, message = "总号源至少为1")
    @Schema(description = "总号源", example = "50", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer capacity;
}
