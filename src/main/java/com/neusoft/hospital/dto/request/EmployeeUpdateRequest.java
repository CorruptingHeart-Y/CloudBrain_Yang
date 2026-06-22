package com.neusoft.hospital.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "员工更新请求")
public class EmployeeUpdateRequest {

    @NotNull(message = "ID不能为空")
    @Schema(description = "员工ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer id;

    @Schema(description = "所属科室ID", example = "1")
    private Integer deptmentId;

    @Schema(description = "挂号级别ID", example = "1")
    private Integer registLevelId;

    @Schema(description = "排班规则ID", example = "1")
    private Integer schedulingId;

    @Schema(description = "真实姓名", example = "李医生")
    private String realname;

    @Schema(description = "密码", example = "123456")
    private String password;
}
