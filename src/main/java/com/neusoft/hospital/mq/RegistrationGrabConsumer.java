package com.neusoft.hospital.mq;

import com.neusoft.hospital.common.BusinessException;
import com.neusoft.hospital.dto.request.RegisterCreateRequest;
import com.neusoft.hospital.entity.Employee;
import com.neusoft.hospital.entity.Patient;
import com.neusoft.hospital.entity.Register;
import com.neusoft.hospital.entity.RegistrationTicket;
import com.neusoft.hospital.mapper.PatientMapper;
import com.neusoft.hospital.service.EmployeeService;
import com.neusoft.hospital.service.QuotaService;
import com.neusoft.hospital.service.RegistrationTicketService;
import com.neusoft.hospital.service.RegisterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 抢号消费者：MySQL 兜底扣减 + 落库 + 票据状态机。
 * <p>幂等：ticket 非 PENDING 直接跳过。业务失败(满号/未放号) → refund Redis + FAILED + ack。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegistrationGrabConsumer {

    private final RegistrationTicketService ticketService;
    private final QuotaService quotaService;
    private final RegisterService registerService;
    private final EmployeeService employeeService;
    private final PatientMapper patientMapper;

    @RabbitListener(queues = "${hospital.registration.queue:regist.grab.queue}")
    @Transactional(rollbackFor = Exception.class)
    // 消费者线程消费消息的逻辑
    public void consume(Integer ticketId) {
        RegistrationTicket ticket = ticketService.getById(ticketId);
        if (ticket == null) {
            //可能是过期消息, 该票据已经被删除了.
            log.warn("抢号票据不存在 ticketId={}，跳过", ticketId);
            return;
        }
        if (!RegistrationTicket.STATUS_PENDING.equals(ticket.getStatus())) {
            //再做一层幂等处理
            log.info("抢号票据已处理 ticketId={} status={}，幂等跳过", ticketId, ticket.getStatus());
            return;
        }

        try {
            // ---- MySQL 兜底扣减（权威库存）----
            boolean deducted = quotaService.deductDbOrThrow(
                    ticket.getEmployeeId(), ticket.getVisitDate(), ticket.getNoon());
            if (!deducted) {
                //返回false说明扣减失败, 说明mysql里根本没有号源行.
                // 未放号（Redis 有库存但 DB 无号源行，数据不一致）→ 失败回补
                quotaService.refundRedis(ticket.getEmployeeId(), ticket.getVisitDate(), ticket.getNoon());
                ticketService.markFailed(ticketId, "未放号");
                return;
            }

            // ---- 组装挂号并落库 ----
            //插入挂号记录
            RegisterCreateRequest req = buildRequest(ticket);
            Register register = registerService.createRegister(req, ticket.getEmployeeId());
                       //标记抢号票据状态为Success.
            ticketService.markSuccess(ticketId, register.getId());
            log.info("抢号成功 ticketId={} registerId={} caseNumber={}",
                    ticketId, register.getId(), register.getCaseNumber());

        } catch (BusinessException e) {
            //捕获到满号异常,标记抢票票据的状态为 FAILED,并写明失败原因为 "号满了"
            // 满号(409) 等：回补 Redis + 标记失败
            quotaService.refundRedis(ticket.getEmployeeId(), ticket.getVisitDate(), ticket.getNoon());
            ticketService.markFailed(ticketId, e.getMessage());
            log.warn("抢号业务失败 ticketId={} reason={}", ticketId, e.getMessage());
        } catch (Exception e) {
            // 未预期异常：回补 Redis + 标记失败 + 吞掉（避免毒消息循环；DLX 留作后续增强）
            quotaService.refundRedis(ticket.getEmployeeId(), ticket.getVisitDate(), ticket.getNoon());
            ticketService.markFailed(ticketId, "系统异常: " + e.getMessage());
            log.error("抢号未预期异常 ticketId={}", ticketId, e);
        }
    }

    private RegisterCreateRequest buildRequest(RegistrationTicket ticket) {
        Patient patient = patientMapper.selectById(ticket.getPatientId());
        Employee doctor = employeeService.getById(ticket.getEmployeeId());

        RegisterCreateRequest req = new RegisterCreateRequest();
        if (patient != null) {
            req.setRealName(patient.getRealName());
            req.setGender(patient.getGender());
            req.setCardNumber(patient.getCardNumber());
            req.setBirthdate(patient.getBirthdate());
            req.setHomeAddress(patient.getHomeAddress());
            if (patient.getBirthdate() != null) {
                req.setAge(calcAge(patient.getBirthdate()));
                req.setAgeType("岁");
            }
        } else {
            // 极端情况：患者档案缺失，用票据最小信息兜底
            req.setRealName("患者" + ticket.getPatientId());
            req.setGender("男");
        }
        req.setVisitDate(toVisitDateTime(ticket.getVisitDate(), ticket.getNoon()));
        req.setNoon(ticket.getNoon());
        req.setDeptmentId(doctor != null ? doctor.getDeptmentId() : null);
        req.setRegistLevelId(ticket.getRegistLevelId());
        req.setSettleCategoryId(ticket.getSettleCategoryId());
        req.setIsBook("否");
        req.setRegistMethod("网上抢号");
        return req;
    }

    private LocalDateTime toVisitDateTime(LocalDate date, String noon) {
        // 上午 09:00 / 下午 14:00
        int hour = "下午".equals(noon) ? 14 : 9;
        return date.atTime(hour, 0);
    }

    private Integer calcAge(LocalDate birthdate) {
        return java.time.Period.between(birthdate, LocalDate.now()).getYears();
    }
}
