package com.neusoft.hospital.config;

import io.lettuce.core.ClientOptions;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Lettuce 连接加固 — 防止 Redis 容器重启后拿到死连接导致 Connection reset。
     * <ul>
     *   <li>{@code autoReconnect} — 连接断开后自动重连（默认开启，显式写出更明确）</li>
     *   <li>{@code pingBeforeActivateConnection} — 从池里拿出连接前先发 PING，
     *       等同于 Jedis 的 testOnBorrow，不存活则重建连接</li>
     * </ul>
     */
    @Bean
    public LettuceClientConfigurationBuilderCustomizer lettuceCustomizer() {
        return builder -> builder.clientOptions(
            ClientOptions.builder()
                .autoReconnect(true)
                .pingBeforeActivateConnection(true)
                .build()
        );
    }
}
