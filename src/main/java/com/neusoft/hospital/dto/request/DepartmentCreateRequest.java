package com.neusoft.hospital.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "科室创建请求")
public class DepartmentCreateRequest {

    @NotBlank(message = "科室编码不能为空")
    @Schema(description = "科室编码", example = "SJNK", requiredMode = Schema.RequiredMode.REQUIRED)
    private String deptCode;

    @NotBlank(message = "科室名称不能为空")
    @Schema(description = "科室名称", example = "神经内科", requiredMode = Schema.RequiredMode.REQUIRED)
    private String deptName;

    @Schema(description = "科室类型", example = "临床科室")
    private String deptType;
}
