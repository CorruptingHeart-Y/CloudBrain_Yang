package com.neusoft.hospital.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "科室响应DTO")
public class DepartmentResponse {

    @Schema(description = "科室ID", example = "1")
    private Integer id;

    @Schema(description = "科室编码", example = "SJNK")
    private String deptCode;

    @Schema(description = "科室名称", example = "神经内科")
    private String deptName;

    @Schema(description = "科室类型", example = "临床科室")
    private String deptType;

    @Schema(description = "删除标记", example = "1")
    private Integer delmark;
}
