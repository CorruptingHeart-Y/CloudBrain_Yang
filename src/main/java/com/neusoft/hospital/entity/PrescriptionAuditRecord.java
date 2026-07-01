package com.neusoft.hospital.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 处方审核记录表
 * 承接 AI 处方辅助审核的输入快照与结果留痕；register_id 指向挂号，auditor_employee_id 记操作员。
 * 触发：①手动预览不落库；②确认/保存处方时审核并落本表（见 AI 接入决策3）。
 */
@Data
@TableName("prescription_audit_record")
@Schema(description = "处方审核记录表")
public class PrescriptionAuditRecord {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID", example = "1")
    private Integer id;

    @Schema(description = "挂号ID", example = "1")
    private Integer registerId;

    @Schema(description = "审核请求快照JSON(患者信息+药品明细)")
    private String requestSnapshot;

    @Schema(description = "AI完整返回JSON(含suggestions/interactions/riskItems)")
    private String resultJson;

    @Schema(description = "总体风险等级：low/medium/high", example = "medium")
    private String riskLevel;

    @Schema(description = "审核操作员员工ID", example = "1")
    private Integer auditorEmployeeId;

    @Schema(description = "创建时间", example = "2026-07-01T10:30:00")
    private LocalDateTime creationTime;
}
