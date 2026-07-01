package com.neusoft.hospital.ai.service;

import com.neusoft.hospital.ai.dto.TriageConsultRequest;
import com.neusoft.hospital.ai.dto.TriageResultDTO;

/**
 * 诊前分诊服务：装配候选科室/医生 → 调 Python AI → 富化结果 → 落 triage_record。
 */
public interface TriageService {

    /**
     * 诊前分诊咨询。
     * AI 不可用时抛 BusinessException(AI_UNAVAILABLE)，不落记录。
     */
    TriageResultDTO consult(TriageConsultRequest request);
}
