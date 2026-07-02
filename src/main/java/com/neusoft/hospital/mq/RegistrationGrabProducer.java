package com.neusoft.hospital.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 抢号消息生产者：Redis 扣减成功后投递 ticketId 到主队列。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegistrationGrabProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${hospital.registration.exchange:regist.exchange}")
    private String exchange;
    @Value("${hospital.registration.routing-key:regist.grab}")
    private String routingKey;

    public void send(Integer ticketId) {
        rabbitTemplate.convertAndSend(exchange, routingKey, ticketId);
        log.info("抢号消息已投递 ticketId={}", ticketId);
    }
}
