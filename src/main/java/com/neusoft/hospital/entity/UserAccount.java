package com.neusoft.hospital.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 统一认证账号表
 */
@Data
@TableName("user_account")
@Schema(description = "统一认证账号表")
public class UserAccount {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID", example = "1")
    private Integer id;

    @Schema(description = "登录账号（唯一）", example = "admin")
    private String username;

    @Schema(description = "密码（BCrypt 哈希）", example = "$2a$10$N9qo8uLOickgx2ZMRZoMy...")
    private String password;

    @Schema(description = "角色：ADMIN/DOCTOR/PATIENT", example = "DOCTOR")
    private String role;

    @Schema(description = "关联员工ID，DOCTOR/ADMIN 使用", example = "1")
    private Integer employeeId;

    @Schema(description = "关联患者ID，PATIENT 使用", example = "1")
    private Integer patientId;

    @Schema(description = "状态：1-启用 0-禁用", example = "1")
    private Integer status;

    @TableLogic
    @Schema(description = "删除标记：1-正常 0-已删除", example = "1")
    private Integer delmark;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
