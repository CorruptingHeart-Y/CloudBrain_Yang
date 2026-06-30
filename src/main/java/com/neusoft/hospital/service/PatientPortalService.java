package com.neusoft.hospital.service;

import com.neusoft.hospital.common.PageResult;
import com.neusoft.hospital.dto.response.PatientProfileResponse;
import com.neusoft.hospital.dto.response.PatientRecordDetailResponse;
import com.neusoft.hospital.dto.response.PatientRecordSummaryResponse;

/**
 * 患者门户服务（PR3）。
 * <p>
 * 所有方法的患者身份只来自 {@link com.neusoft.hospital.auth.context.CurrentUser}，
 * 绝不接收前端传入的 patientId / employeeId。数据归属在 Service 层以 SQL 条件
 * register.patient_id = 当前患者 强制落实，不依赖 Controller 校验。
 */
public interface PatientPortalService {

    /** 当前患者非敏感展示资料。 */
    PatientProfileResponse getCurrentPatientProfile();

    /** 分页查询当前患者的就诊记录摘要（仅 register.patient_id = 当前患者）。 */
    PageResult<PatientRecordSummaryResponse> pageCurrentPatientRecords(Integer pageNum, Integer pageSize);

    /**
     * 当前患者单次就诊详情。
     * <p>
     * 先以 register.id = registerId AND register.patient_id = 当前患者 联合查询；
     * 无命中 → 抛 404；命中后才按 register_id 读取病历/处方/检查/检验/处置。
     */
    PatientRecordDetailResponse getCurrentPatientRecordDetail(Integer registerId);
}
