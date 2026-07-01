package com.neusoft.hospital.ai.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * 调用 Python AI 服务的 RestClient。
 * 默认带 X-Internal-Key 头，供 Python 中间件校验；baseUrl + 超时统一在此配置。
 */
@Configuration
@RequiredArgsConstructor
public class RestClientConfig {

    private final AiProperties aiProperties;

    @Bean
    public RestClient aiRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(aiProperties.getTimeoutSeconds() * 1000);
        factory.setReadTimeout(aiProperties.getTimeoutSeconds() * 1000);

        return RestClient.builder()
                .baseUrl(aiProperties.getBaseUrl())
                .defaultHeader("X-Internal-Key", aiProperties.getInternalKey())
                .requestFactory(factory)
                .build();
    }
}
