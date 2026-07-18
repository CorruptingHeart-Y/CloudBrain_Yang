package com.neusoft.hospital.service.impl;

import com.neusoft.hospital.auth.context.CurrentUser;
import com.neusoft.hospital.common.BusinessException;
import com.neusoft.hospital.common.ErrorCode;
import com.neusoft.hospital.dto.request.RegistrationGrabRequest;
import com.neusoft.hospital.dto.response.RegistrationGrabResponse;
import com.neusoft.hospital.dto.response.RegistrationTicketResponse;
import com.neusoft.hospital.entity.Register;
import com.neusoft.hospital.entity.RegistrationTicket;
import com.neusoft.hospital.mq.RegistrationGrabProducer;
import com.neusoft.hospital.service.QuotaService;
import com.neusoft.hospital.service.RegistrationGrabService;
import com.neusoft.hospital.service.RegistrationTicketService;
import com.neusoft.hospital.service.RegisterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * 抢号编排：幂等(一人一号) → Redis Lua 扣减 → 投递 MQ 异步落库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationGrabServiceImpl implements RegistrationGrabService {

    private final RegistrationTicketService ticketService;
    private final QuotaService quotaService;
    private final RegistrationGrabProducer producer;
    private final RegisterService registerService;
  //核心并发抢号业务方法
    @Override
    public RegistrationGrabResponse grab(RegistrationGrabRequest request) {
        //从ThreadLocal必须要能拿到patientId,否则就是非法请求
        Integer patientId = CurrentUser.requireAuthUser().getPatientId();
        if (patientId == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN.getCode(), "当前账号未绑定患者档案，无法抢号");
        }

        // ---- 幂等：一人一号（每半日）----
        RegistrationTicket existing = ticketService.getByPatientDateNoon(
                patientId, request.getVisitDate(), request.getNoon());
        if (existing != null) {
            if (RegistrationTicket.STATUS_PENDING.equals(existing.getStatus())
                    || RegistrationTicket.STATUS_SUCCESS.equals(existing.getStatus())) {
                // 已有进行中/成功的号 → 直接返回（幂等）
                RegistrationGrabResponse r = new RegistrationGrabResponse();
                r.setTicketNo(existing.getTicketNo());
                r.setStatus(existing.getStatus());
                return r;
            }
            // FAILED / CANCELLED → 删旧后允许重抢
            ticketService.removeById(existing.getId());
        }

        // ---- 建 PENDING 票据 ----
        RegistrationTicket ticket;
        try {
            //执行的是insert语句, 就算同一个患者的两个几乎同时到达的请求同时穿透了前面的幂等判断,
            //在数据库执行insert语句也会被行锁强行同步, 只有一个线程能insert成功,另一个线程会因为试图插入具有唯一索引的一行数据而抛出
            //DuplicateKeyException(唯一键冲突异常)而导致插入失败,这里再做了一层幂等处理.
            ticket = ticketService.createPending(patientId, request.getEmployeeId(),
                    request.getVisitDate(), request.getNoon(),
                    request.getRegistLevelId(), request.getSettleCategoryId());
        } catch (DuplicateKeyException e) {
            // 并发双击：唯一约束兜底 → 回查返回既有
            RegistrationTicket cur = ticketService.getByPatientDateNoon(
                    patientId, request.getVisitDate(), request.getNoon());
            RegistrationGrabResponse r = new RegistrationGrabResponse();
            r.setTicketNo(cur.getTicketNo());
            r.setStatus(cur.getStatus());
            return r;
        }

        // ---- Redis Lua 原子扣减（快速过滤）----
        int r = quotaService.tryDeduct(request.getEmployeeId(), request.getVisitDate(), request.getNoon());
        if (r == -1) {
            ticketService.markFailed(ticket.getId(), "未放号");
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "该医生当日未放号，无法抢号");
        }
        if (r == 0) {
            ticketService.markFailed(ticket.getId(), "已约满");
            throw new BusinessException(ErrorCode.CONFLICT.getCode(), "该号源已约满");
        }

        // ---- 扣减成功 → 投递 MQ 异步落库 ----
        // 消息包含  生成的票据的id,根据这个id,消费者消息可以异步消费这条消息, 即执行真正的mysql落库.
        try {
            producer.send(ticket.getId());
        } catch (AmqpException e) {
            // MQ 不可用时，Redis 已经预扣成功；这里必须补回库存并终止票据，避免 PENDING 长时间悬挂。
            quotaService.refundRedis(request.getEmployeeId(), request.getVisitDate(), request.getNoon());
            ticketService.markFailed(ticket.getId(), "消息队列不可用，抢号失败");
            log.error("抢号消息投递失败 ticketId={}", ticket.getId(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR.getCode(), "消息队列暂不可用，请稍后重试");
        }

        //消息投递成功后,就可以直接给前端返回 正在处理的消息. 不会阻塞前端的请求, 提升用户体验.
        RegistrationGrabResponse resp = new RegistrationGrabResponse();
        resp.setTicketNo(ticket.getTicketNo());
        resp.setStatus(RegistrationTicket.STATUS_PENDING);
        return resp;
    }

    @Override
    public RegistrationTicketResponse getResult(String ticketNo) {
        Integer patientId = CurrentUser.requireAuthUser().getPatientId();
        RegistrationTicket t = ticketService.getByTicketNo(ticketNo);
        if (t == null || !t.getPatientId().equals(patientId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "票据不存在");
        }
        RegistrationTicketResponse resp = new RegistrationTicketResponse();
        resp.setTicketNo(t.getTicketNo());
        resp.setEmployeeId(t.getEmployeeId());
        resp.setVisitDate(t.getVisitDate());
        resp.setNoon(t.getNoon());
        resp.setStatus(t.getStatus());
        resp.setRegisterId(t.getRegisterId());
        resp.setFailReason(t.getFailReason());
        resp.setCreateTime(t.getCreateTime());
        if (t.getRegisterId() != null) {
            Register reg = registerService.getById(t.getRegisterId());
            if (reg != null) {
                resp.setCaseNumber(reg.getCaseNumber());
            }
        }
        return resp;
    }
}
