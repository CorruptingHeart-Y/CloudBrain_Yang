package com.neusoft.hospital.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 患者自助注册请求（@Public）。
 * <p>
 * cardNumber 仅做 18 位格式合规校验，不声称真实身份证核验；课程项目级身份核验。
 */
@Data
@Schema(description = "患者自助注册请求")
public class PatientRegisterRequest {

    @NotBlank(message = "账号不能为空")
    @Size(min = 3, max = 64, message = "账号长度需在3-64之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "账号仅允许字母、数字、下划线")
    @Schema(description = "登录账号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 72, message = "密码长度需在6-72之间")
    @Schema(description = "密码(至少6位)", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @NotBlank(message = "姓名不能为空")
    @Schema(description = "真实姓名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String realName;

    @NotBlank(message = "性别不能为空")
    @Schema(description = "性别：男/女", requiredMode = Schema.RequiredMode.REQUIRED)
    private String gender;

    @NotNull(message = "出生日期不能为空")
    @Schema(description = "出生日期", example = "1990-01-01", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate birthdate;

    @NotBlank(message = "身份证号不能为空")
    @Pattern(regexp = "^[0-9]{17}[0-9Xx]$", message = "身份证号格式不合规")
    @Schema(description = "身份证号(仅做18位格式合规校验，非真实身份核验)", requiredMode = Schema.RequiredMode.REQUIRED)
    private String cardNumber;

    @Schema(description = "手机号(可选)")
    private String phone;

    @Schema(description = "家庭住址(可选)")
    private String homeAddress;
}
