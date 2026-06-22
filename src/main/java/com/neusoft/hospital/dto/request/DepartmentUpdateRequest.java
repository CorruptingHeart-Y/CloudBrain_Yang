package com.neusoft.hospital.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "科室更新请求")
public class DepartmentUpdateRequest {

    @NotNull(message = "ID不能为空")
    @Schema(description = "科室ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer id;

    @Schema(description = "科室编码", example = "SJNK")
    private String deptCode;

    @Schema(description = "科室名称", example = "神经内科")
    private String deptName;

    @Schema(description = "科室类型", example = "临床科室")
    private String deptType;
}
