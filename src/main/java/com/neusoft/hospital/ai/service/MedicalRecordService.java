package com.neusoft.hospital.ai.service;

import com.neusoft.hospital.ai.dto.MedicalRecordDraftDTO;
import com.neusoft.hospital.ai.dto.MedicalRecordDTO;
import com.neusoft.hospital.ai.dto.MedicalRecordSaveRequest;

/**
 * AI 病历生成与持久化。
 * ① generate：医患对话文本 → AI 出 9 字段草稿，仅预览不落库；
 * ② save：医生确认（可能编辑过）的病历落 medical_record + 替换 medical_record_disease 关联；
 * ③ getByRegisterId：按挂号查已落库病历。
 * AI 失败抛 BusinessException(AI_MEDICAL_UNAVAILABLE)，由全局异常处理转 503 提示人工书写。
 */
public interface MedicalRecordService {

    /** 根据挂号ID + 医患对话文本生成病历草稿（不落库）。 */
    MedicalRecordDraftDTO generate(Integer registerId, String dialogue);

    /** 保存/确认病历：upsert medical_record + 替换疾病关联，返回落库后病历。 */
    MedicalRecordDTO save(MedicalRecordSaveRequest request);

    /** 按挂号ID查询已落库病历（含关联疾病）；不存在返回 null。 */
    MedicalRecordDTO getByRegisterId(Integer registerId);
}
