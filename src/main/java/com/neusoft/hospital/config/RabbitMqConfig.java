package com.neusoft.hospital.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 拓扑：抢号主队列 + 死信队列(DLX)。
 * <p>消费失败(default-requeue-rejected=false)经 retry 后进入 DLX 人工排查。
 */
@Configuration
public class RabbitMqConfig {

    @Value("${hospital.registration.exchange:regist.exchange}")
    private String exchange;
    @Value("${hospital.registration.queue:regist.grab.queue}")
    private String queue;
    @Value("${hospital.registration.routing-key:regist.grab}")
    private String routingKey;
    @Value("${hospital.registration.dlx-exchange:regist.dlx.exchange}")
    private String dlxExchange;
    @Value("${hospital.registration.dlx-queue:regist.dlx.queue}")
    private String dlxQueue;
    @Value("${hospital.registration.dlx-routing-key:regist.dlx}")
    private String dlxRoutingKey;

    @Bean
    public DirectExchange registExchange() {
        return new DirectExchange(exchange, true, false);
    }

    @Bean
    public Queue registQueue() {
        return QueueBuilder.durable(queue)
                .withArgument("x-dead-letter-exchange", dlxExchange)
                .withArgument("x-dead-letter-routing-key", dlxRoutingKey)
                .build();
    }

    @Bean
    public Binding registBinding() {
        return BindingBuilder.bind(registQueue()).to(registExchange()).with(routingKey);
    }

    @Bean
    public DirectExchange registDlxExchange() {
        return new DirectExchange(dlxExchange, true, false);
    }

    @Bean
    public Queue registDlxQueue() {
        return QueueBuilder.durable(dlxQueue).build();
    }

    @Bean
    public Binding registDlxBinding() {
        return BindingBuilder.bind(registDlxQueue()).to(registDlxExchange()).with(dlxRoutingKey);
    }

    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
