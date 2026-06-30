package com.neusoft.hospital.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 诊前分诊记录表
 * 承接 AI 诊前分诊推理结果留痕；患者身份沿用 register.card_number + case_number，
 * 挂号前无 register 时直接落患者简要信息，操作员记 employee_id。
 */
@Data
@TableName("triage_record")
@Schema(description = "诊前分诊记录表")
public class TriageRecord {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID", example = "1")
    private Integer id;

    @Schema(description = "患者身份证号", example = "210102199001011234")
    private String cardNumber;

    @Schema(description = "病历号", example = "BL20260601001")
    private String caseNumber;

    @Schema(description = "患者姓名", example = "李四")
    private String patientName;

    @Schema(description = "性别：男/女", example = "男")
    private String gender;

    @Schema(description = "年龄", example = "45")
    private Integer age;

    @Schema(description = "主诉", example = "头疼三天，伴有恶心")
    private String chiefComplaint;

    @Schema(description = "推荐科室ID列表(逗号分隔)", example = "3,7")
    private String recommendDeptIds;

    @Schema(description = "推荐医生ID列表(逗号分隔)", example = "12,5")
    private String recommendDoctorIds;

    @Schema(description = "AI完整返回JSON(含reason/score)")
    private String aiRawResult;

    @Schema(description = "操作员员工ID", example = "1")
    private Integer operatorEmployeeId;

    @Schema(description = "创建时间", example = "2026-06-30T18:00:00")
    private LocalDateTime creationTime;
}
