package com.neusoft.hospital.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "创建患者账号请求(PATIENT)")
public class AccountCreatePatientRequest {

    @NotBlank(message = "账号不能为空")
    @Size(min = 3, max = 64, message = "账号长度需在3-64之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "账号仅允许字母、数字、下划线")
    @Schema(description = "登录账号", example = "patient03", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 72, message = "密码长度需在6-72之间")
    @Schema(description = "密码(BCrypt入库，至少6位)", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @NotNull(message = "患者ID不能为空")
    @Schema(description = "绑定的患者ID，必须存在且未绑定其他账号", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer patientId;
}
