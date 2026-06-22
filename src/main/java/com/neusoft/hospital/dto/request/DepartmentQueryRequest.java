package com.neusoft.hospital.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "科室查询请求")
public class DepartmentQueryRequest {

    @Schema(description = "科室名称(模糊查询)", example = "神经")
    private String deptName;

    @Schema(description = "科室类型", example = "临床科室")
    private String deptType;
}
