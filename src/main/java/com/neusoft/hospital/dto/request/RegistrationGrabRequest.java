package com.neusoft.hospital.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "患者抢号请求")
public class RegistrationGrabRequest {

    @NotNull(message = "医生ID不能为空")
    @Schema(description = "医生ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer employeeId;

    @NotNull(message = "就诊日期不能为空")
    @Schema(description = "就诊日期", example = "2026-07-03", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate visitDate;

    @NotBlank(message = "午别不能为空")
    @Schema(description = "午别：上午/下午", example = "上午", requiredMode = Schema.RequiredMode.REQUIRED)
    private String noon;

    @NotNull(message = "挂号级别ID不能为空")
    @Schema(description = "挂号级别ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer registLevelId;

    @NotNull(message = "结算类别ID不能为空")
    @Schema(description = "结算类别ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer settleCategoryId;
}
