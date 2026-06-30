package com.neusoft.hospital.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 患者侧就诊记录摘要（PR3）。
 * <p>
 * 仅含就诊摘要字段；刻意排除 cardNumber / homeAddress 等敏感字段，不直接暴露 Register 实体。
 * 数据范围由 Service 层按 register.patient_id = 当前患者 强制限定。
 */
@Data
@Schema(description = "患者就诊记录摘要响应")
public class PatientRecordSummaryResponse {

    @Schema(description = "挂号ID", example = "1")
    private Integer registerId;

    @Schema(description = "病历号", example = "BL20260601001")
    private String caseNumber;

    @Schema(description = "就诊日期时间", example = "2026-06-01T09:30:00")
    private LocalDateTime visitDate;

    @Schema(description = "午别：上午/下午", example = "上午")
    private String noon;

    @Schema(description = "科室名称", example = "神经内科")
    private String deptName;

    @Schema(description = "接诊医生姓名", example = "测试医生")
    private String employeeName;

    @Schema(description = "挂号级别名称", example = "普通号")
    private String registLevelName;

    @Schema(description = "看诊状态：1-已挂号 2-医生接诊 3-看诊结束 4-已退号", example = "1")
    private Integer visitState;
}
