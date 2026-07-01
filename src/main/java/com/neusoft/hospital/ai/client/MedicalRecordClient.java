package com.neusoft.hospital.ai.client;

import com.neusoft.hospital.ai.config.AiProperties;
import com.neusoft.hospital.ai.dto.AiMedicalRecordRequest;
import com.neusoft.hospital.ai.dto.AiMedicalRecordResponse;
import com.neusoft.hospital.common.BusinessException;
import com.neusoft.hospital.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * 调用 Python /ai/medical-record 的客户端。
 * 异常映射：
 * - 401（X-Internal-Key 不一致）→ 系统错误，提示密钥配置问题（开发期排查）。
 * - 503 / 超时 / 连接失败 → AI_MEDICAL_UNAVAILABLE，上层据此向前端返回"请人工书写"。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MedicalRecordClient {

    @Qualifier("aiRestClient")
    private final RestClient aiRestClient;
    private final AiProperties aiProperties;

    public AiMedicalRecordResponse generate(AiMedicalRecordRequest request) {
        try {
            return aiRestClient.post()
                    .uri(aiProperties.getMedicalRecordPath())
                    .body(request)
                    .retrieve()
                    .body(AiMedicalRecordResponse.class);
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("AI服务内部密钥校验失败(401)：{}", e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR.getCode(),
                    "AI服务内部密钥校验失败，请检查 hospital.ai.internal-key 与 ai-service/.env 的 INTERNAL_KEY 是否一致");
        } catch (HttpServerErrorException e) {
            log.warn("AI病历生成服务返回错误状态 {}：{}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.AI_MEDICAL_UNAVAILABLE);
        } catch (ResourceAccessException e) {
            log.warn("AI病历生成服务不可达：{}", e.getMessage());
            throw new BusinessException(ErrorCode.AI_MEDICAL_UNAVAILABLE);
        }
    }
}
