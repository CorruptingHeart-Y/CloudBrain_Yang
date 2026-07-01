package com.neusoft.hospital.ai.service;

import com.neusoft.hospital.ai.dto.PrescriptionAuditRecordDTO;
import com.neusoft.hospital.ai.dto.PrescriptionAuditResultDTO;

import java.util.List;

/**
 * AI 处方辅助审核。
 * ① 手动预览：audit(registerId, false)，不落库；
 * ② 确认/保存处方时审核并落库：audit(registerId, true)。
 * AI 失败抛 BusinessException(AI_AUDIT_UNAVAILABLE)，由全局异常处理转 503 提示人工核对，不阻塞开方。
 */
public interface PrescriptionAuditService {

    /**
     * 审核指定挂号下全部处方明细。
     *
     * @param registerId 挂号ID
     * @param persist    true=审核并落 prescription_audit_record；false=仅预览不落库
     */
    PrescriptionAuditResultDTO audit(Integer registerId, boolean persist);

    /**
     * 查询某挂号的处方审核留痕记录（按创建时间倒序）。
     */
    List<PrescriptionAuditRecordDTO> listByRegisterId(Integer registerId);
}
