package com.neusoft.hospital.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "员工查询请求")
public class EmployeeQueryRequest {

    @Schema(description = "真实姓名(模糊查询)", example = "李")
    private String realname;

    @Schema(description = "所属科室ID", example = "1")
    private Integer deptmentId;
}
