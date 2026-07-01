package com.neusoft.hospital.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 患者简要信息（分诊用），对齐 Python schemas.triage.PatientBrief。
 */
@Data
@Schema(description = "患者简要信息")
public class PatientBrief {

    @Schema(description = "年龄", example = "45")
    private Integer age;

    @Schema(description = "性别：男/女", example = "男")
    private String gender;
}
