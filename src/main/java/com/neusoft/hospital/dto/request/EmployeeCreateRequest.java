package com.neusoft.hospital.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "员工创建请求")
public class EmployeeCreateRequest {

    @NotNull(message = "所属科室ID不能为空")
    @Schema(description = "所属科室ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer deptmentId;

    @Schema(description = "挂号级别ID", example = "1")
    private Integer registLevelId;

    @Schema(description = "排班规则ID", example = "1")
    private Integer schedulingId;

    @NotBlank(message = "真实姓名不能为空")
    @Schema(description = "真实姓名", example = "李医生", requiredMode = Schema.RequiredMode.REQUIRED)
    private String realname;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;
}
