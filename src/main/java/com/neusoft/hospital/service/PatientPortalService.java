package com.neusoft.hospital.service;

import com.neusoft.hospital.common.PageResult;
import com.neusoft.hospital.dto.response.PatientProfileResponse;
import com.neusoft.hospital.dto.response.PatientRecordDetailResponse;
import com.neusoft.hospital.dto.response.PatientRecordSummaryResponse;

/**
 * 患者门户服务（v2.0）。
 * <p>
 * 所有方法的患者身份只来自 {@link com.neusoft.hospital.auth.context.CurrentUser}，
 * 绝不接收前端传入的 patientId / employeeId。患者↔挂号归属经 patient_register_link
 * 桥接表表达，不依赖 register.patient_id。
 */
public interface PatientPortalService {

    /** 当前患者非敏感展示资料。 */
    PatientProfileResponse getCurrentPatientProfile();

    /** 分页查询当前患者的就诊记录摘要（仅桥接表内 link 到本人的 register）。 */
    PageResult<PatientRecordSummaryResponse> pageCurrentPatientRecords(Integer pageNum, Integer pageSize);

    /**
     * 当前患者单次就诊详情。
     * <p>
     * 先校验 (patientId, registerId) 在桥接表存在 link；无命中 → 抛 404；
     * 命中后才按 register_id 读取病历/处方/检查/检验/处置。
     */
    PatientRecordDetailResponse getCurrentPatientRecordDetail(Integer registerId);
}
