package com.neusoft.hospital.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "医技项目查询请求")
public class MedicalTechnologyQueryRequest {

    @Schema(description = "项目名称(模糊查询)", example = "CT")
    private String techName;

    @Schema(description = "项目类型", example = "检查")
    private String techType;

    @Schema(description = "科室ID", example = "1")
    private Integer deptmentId;
}
