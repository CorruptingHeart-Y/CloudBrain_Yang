package com.neusoft.hospital.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "登录请求")
public class LoginRequest {

    @NotNull(message = "工号不能为空")
    @Schema(description = "员工工号(employee.id)", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer id;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;
}
