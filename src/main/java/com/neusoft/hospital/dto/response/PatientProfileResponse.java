package com.neusoft.hospital.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 患者侧个人资料响应（PR3）。
 * <p>
 * 仅返回非敏感展示资料；刻意排除 cardNumber / phone / homeAddress / 密码哈希等敏感字段。
 * patientId 来自已验证 JWT / CurrentUser，不作为前端可控输入。
 */
@Data
@Schema(description = "患者个人资料响应")
public class PatientProfileResponse {

    @Schema(description = "患者ID（来自已验证身份，非前端输入）", example = "1")
    private Integer patientId;

    @Schema(description = "姓名", example = "测试患者")
    private String realName;

    @Schema(description = "性别", example = "男")
    private String gender;

    @Schema(description = "出生日期", example = "1990-01-01")
    private LocalDate birthdate;
}
