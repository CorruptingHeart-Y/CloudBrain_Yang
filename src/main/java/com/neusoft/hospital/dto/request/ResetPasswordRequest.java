package com.neusoft.hospital.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "重置密码请求")
public class ResetPasswordRequest {

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 72, message = "密码长度需在6-72之间")
    @Schema(description = "新密码(BCrypt入库，至少6位)", requiredMode = Schema.RequiredMode.REQUIRED)
    private String newPassword;
}
