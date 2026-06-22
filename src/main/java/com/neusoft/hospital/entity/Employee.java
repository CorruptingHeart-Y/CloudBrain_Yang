package com.neusoft.hospital.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 医院员工表
 */
@Data
@TableName("employee")
@Schema(description = "医院员工表")
public class Employee {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID", example = "1")
    private Integer id;

    @Schema(description = "科室ID", example = "10")
    private Integer deptmentId;

    @Schema(description = "挂号级别ID", example = "1")
    private Integer registLevelId;

    @Schema(description = "排班ID", example = "3")
    private Integer schedulingId;

    @Schema(description = "真实姓名", example = "张三")
    private String realname;

    @Schema(description = "密码", example = "123456")
    private String password;

    @TableLogic
    @Schema(description = "删除标记(0-未删除,1-已删除)", example = "0")
    private Integer delmark;
}
