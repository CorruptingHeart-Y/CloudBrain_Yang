package com.neusoft.hospital.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 患者侧单次就诊详情响应（PR3）。
 * <p>
 * 仅当 register.id = registerId 且 register.patient_id = 当前患者 同时命中时才聚合；
 * 归属校验在 Service 层以单条 SQL 完成，绝不先查详情再在内存判断归属。
 * 各关联列表为空即返回空列表，不因缺失报错；不返回其他患者信息或敏感字段。
 */
@Data
@Schema(description = "患者就诊详情响应")
public class PatientRecordDetailResponse {

    @Schema(description = "挂号摘要")
    private RegisterSummary register;

    @Schema(description = "病历（不存在则为 null）")
    private MedicalRecordSummary medicalRecord;

    @Schema(description = "处方列表")
    private List<PrescriptionItem> prescriptions;

    @Schema(description = "检查申请列表")
    private List<CheckItem> checkRequests;

    @Schema(description = "检验申请列表")
    private List<InspectionItem> inspectionRequests;

    @Schema(description = "处置申请列表")
    private List<DisposalItem> disposalRequests;

    @Data
    @Schema(description = "挂号摘要")
    public static class RegisterSummary {
        @Schema(description = "挂号ID", example = "1")
        private Integer registerId;
        @Schema(description = "病历号", example = "BL20260601001")
        private String caseNumber;
        @Schema(description = "就诊日期时间", example = "2026-06-01T09:30:00")
        private LocalDateTime visitDate;
        @Schema(description = "午别", example = "上午")
        private String noon;
        @Schema(description = "科室名称", example = "神经内科")
        private String deptName;
        @Schema(description = "接诊医生姓名", example = "测试医生")
        private String employeeName;
        @Schema(description = "挂号级别名称", example = "普通号")
        private String registLevelName;
        @Schema(description = "看诊状态", example = "1")
        private Integer visitState;
    }

    @Data
    @Schema(description = "病历摘要")
    public static class MedicalRecordSummary {
        @Schema(description = "主诉", example = "头痛三天")
        private String readme;
        @Schema(description = "现病史")
        private String present;
        @Schema(description = "现病治疗情况")
        private String presentTreat;
        @Schema(description = "既往史")
        private String history;
        @Schema(description = "过敏史")
        private String allergy;
        @Schema(description = "体格检查")
        private String physique;
        @Schema(description = "检查/检验建议")
        private String proposal;
        @Schema(description = "注意事项")
        private String careful;
        @Schema(description = "诊断结果")
        private String diagnosis;
        @Schema(description = "处理意见")
        private String cure;
    }

    @Data
    @Schema(description = "处方项")
    public static class PrescriptionItem {
        @Schema(description = "处方ID", example = "1")
        private Integer id;
        @Schema(description = "药品ID", example = "100")
        private Integer drugId;
        @Schema(description = "药品用法", example = "口服，一日三次")
        private String drugUsage;
        @Schema(description = "药品数量", example = "30")
        private String drugNumber;
        @Schema(description = "创建时间", example = "2026-06-01T10:30:00")
        private LocalDateTime creationTime;
        @Schema(description = "药品状态(0-未取药,1-已取药)", example = "0")
        private String drugState;
    }

    @Data
    @Schema(description = "检查申请项")
    public static class CheckItem {
        @Schema(description = "检查申请ID", example = "1")
        private Integer id;
        @Schema(description = "检查信息", example = "胸部CT检查")
        private String checkInfo;
        @Schema(description = "检查部位", example = "胸部")
        private String checkPosition;
        @Schema(description = "创建时间", example = "2026-06-01T10:00:00")
        private LocalDateTime creationTime;
        @Schema(description = "检查时间", example = "2026-06-01T14:30:00")
        private LocalDateTime checkTime;
        @Schema(description = "检查结果", example = "未见明显异常")
        private String checkResult;
        @Schema(description = "检查状态(0-未检,1-已检)", example = "0")
        private String checkState;
        @Schema(description = "检查备注")
        private String checkRemark;
    }

    @Data
    @Schema(description = "检验申请项")
    public static class InspectionItem {
        @Schema(description = "检验申请ID", example = "1")
        private Integer id;
        @Schema(description = "检验信息", example = "血常规检验")
        private String inspectionInfo;
        @Schema(description = "检验部位", example = "静脉血")
        private String inspectionPosition;
        @Schema(description = "创建时间", example = "2026-06-01T10:00:00")
        private LocalDateTime creationTime;
        @Schema(description = "检验时间", example = "2026-06-01T15:00:00")
        private LocalDateTime inspectionTime;
        @Schema(description = "检验结果", example = "各项指标正常")
        private String inspectionResult;
        @Schema(description = "检验状态(0-未检,1-已检)", example = "0")
        private String inspectionState;
        @Schema(description = "检验备注")
        private String inspectionRemark;
    }

    @Data
    @Schema(description = "处置申请项")
    public static class DisposalItem {
        @Schema(description = "处置申请ID", example = "1")
        private Integer id;
        @Schema(description = "处置信息", example = "清创缝合术")
        private String disposalInfo;
        @Schema(description = "处置部位", example = "左手前臂")
        private String disposalPosition;
        @Schema(description = "创建时间", example = "2026-06-01T10:00:00")
        private LocalDateTime creationTime;
        @Schema(description = "处置时间", example = "2026-06-01T16:00:00")
        private LocalDateTime disposalTime;
        @Schema(description = "处置结果", example = "缝合完成，恢复良好")
        private String disposalResult;
        @Schema(description = "处置状态(0-未处置,1-已处置)", example = "0")
        private String disposalState;
        @Schema(description = "处置备注")
        private String disposalRemark;
    }
}
