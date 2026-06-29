package com.neusoft.hospital.auth.dto;

import com.neusoft.hospital.auth.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "当前登录用户信息")
public class UserInfoResponse {

    @Schema(description = "账号ID(user_account.id)", example = "101")
    private Integer accountId;

    @Schema(description = "角色：ADMIN/DOCTOR/PATIENT", example = "DOCTOR")
    private Role role;

    @Schema(description = "关联员工ID(DOCTOR/ADMIN 使用，PATIENT 为 null)", example = "12")
    private Integer employeeId;

    @Schema(description = "关联患者ID(PATIENT 使用，其余为 null)", example = "null")
    private Integer patientId;

    // ---------- 旧字段（保留语义：id = employeeId，不改为 accountId） ----------

    @Schema(description = "员工ID(旧字段，等价于 employeeId；PATIENT/无关联员工的 ADMIN 为 null)", example = "1")
    private Integer id;

    @Schema(description = "真实姓名", example = "李医生")
    private String realname;

    @Schema(description = "所在科室ID", example = "1")
    private Integer deptmentId;

    @Schema(description = "所在科室名称", example = "神经内科")
    private String deptName;

    @Schema(description = "挂号级别ID", example = "1")
    private Integer registLevelId;

    @Schema(description = "挂号级别名称", example = "普通号")
    private String registLevelName;
}
