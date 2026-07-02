package com.neusoft.hospital.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "挂号创建请求")
public class RegisterCreateRequest {

    @NotBlank(message = "真实姓名不能为空")
    @Schema(description = "真实姓名", example = "张三", requiredMode = Schema.RequiredMode.REQUIRED)
    private String realName;

    @NotBlank(message = "性别不能为空")
    @Schema(description = "性别", example = "男", requiredMode = Schema.RequiredMode.REQUIRED)
    private String gender;

    @Schema(description = "身份证号", example = "210102199001011234")
    private String cardNumber;

    @Schema(description = "出生日期", example = "1990-01-01")
    private LocalDate birthdate;

    @Schema(description = "年龄", example = "34")
    private Integer age;

    @Schema(description = "年龄类型", example = "年")
    private String ageType;

    @Schema(description = "家庭住址", example = "沈阳市和平区xx路")
    private String homeAddress;

    @Schema(description = "看诊日期", example = "2024-06-15T09:00:00")
    private LocalDateTime visitDate;

    @Schema(description = "午别", example = "上午")
    private String noon;

    @NotNull(message = "科室ID不能为空")
    @Schema(description = "科室ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer deptmentId;

    @Schema(description = "医生ID", example = "5")
    private Integer employeeId;

    @NotNull(message = "挂号级别ID不能为空")
    @Schema(description = "挂号级别ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer registLevelId;

    @NotNull(message = "结算类别ID不能为空")
    @Schema(description = "结算类别ID", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer settleCategoryId;

    @Schema(description = "是否记账", example = "否")
    private String isBook;

    @Schema(description = "挂号方式", example = "医保卡")
    private String registMethod;
}
