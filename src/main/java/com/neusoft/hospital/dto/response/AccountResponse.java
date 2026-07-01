package com.neusoft.hospital.dto.response;

import com.neusoft.hospital.auth.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 账号响应 DTO（列表/详情通用）。
 * <p>
 * 刻意排除 password / passwordHash / cardNumber / phone / homeAddress 等敏感字段。
 */
@Data
@Schema(description = "账号响应(不含敏感字段)")
public class AccountResponse {

    @Schema(description = "账号ID", example = "1")
    private Integer accountId;

    @Schema(description = "登录账号", example = "doctor01")
    private String username;

    @Schema(description = "角色", example = "DOCTOR")
    private Role role;

    @Schema(description = "状态：1-启用 0-禁用", example = "1")
    private Integer status;

    @Schema(description = "绑定员工ID", example = "1")
    private Integer employeeId;

    @Schema(description = "绑定患者ID", example = "null")
    private Integer patientId;

    @Schema(description = "展示名(员工/患者真实姓名)", example = "测试医生")
    private String displayName;

    @Schema(description = "Token版本号", example = "1")
    private Integer tokenVersion;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "更新时间")
    private LocalDateTime updatedTime;
}
