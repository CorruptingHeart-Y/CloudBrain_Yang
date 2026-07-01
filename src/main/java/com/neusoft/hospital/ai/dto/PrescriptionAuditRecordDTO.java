package com.neusoft.hospital.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 处方审核记录（返前端）。
 * requestSnapshot / resultJson 为落库时的 JSON 字符串，前端按需 JSON.parse。
 */
@Data
@Schema(description = "处方审核记录")
public class PrescriptionAuditRecordDTO {

    @Schema(description = "主键ID", example = "1")
    private Integer id;

    @Schema(description = "挂号ID", example = "14")
    private Integer registerId;

    @Schema(description = "总体风险等级：low/medium/high", example = "medium")
    private String riskLevel;

    @Schema(description = "审核操作员员工ID", example = "1")
    private Integer auditorEmployeeId;

    @Schema(description = "创建时间", example = "2026-07-01T19:30:00")
    private LocalDateTime creationTime;

    @Schema(description = "审核请求快照JSON(患者信息+药品明细)")
    private String requestSnapshot;

    @Schema(description = "AI完整返回JSON(含suggestions/interactions/riskItems)")
    private String resultJson;
}
