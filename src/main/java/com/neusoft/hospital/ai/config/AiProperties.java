package com.neusoft.hospital.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 服务配置：Python FastAPI 推理服务的地址、内部共享密钥、超时。
 * internal-key 必须与 ai-service/.env 的 INTERNAL_KEY 一致，否则 Python 中间件返回 401。
 */
@Data
@Component
@ConfigurationProperties(prefix = "hospital.ai")
public class AiProperties {

    /** Python AI 服务基础地址 */
    private String baseUrl = "http://localhost:8000";

    /** 与 Python 共享的内部调用密钥 */
    private String internalKey = "change-me-shared-with-springboot";

    /** 分诊端点路径 */
    private String triagePath = "/ai/triage";

    /** 调用超时(秒) */
    private int timeoutSeconds = 30;
}
