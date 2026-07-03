package com.neusoft.hospital.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neusoft.hospital.entity.RegistrationTicket;
import com.neusoft.hospital.mapper.RegistrationTicketMapper;
import com.neusoft.hospital.service.RegistrationTicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class RegistrationTicketServiceImpl extends ServiceImpl<RegistrationTicketMapper, RegistrationTicket>
        implements RegistrationTicketService {

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Override
    public RegistrationTicket getByTicketNo(String ticketNo) {
        return this.getOne(new LambdaQueryWrapper<RegistrationTicket>()
                .eq(RegistrationTicket::getTicketNo, ticketNo));
    }

    @Override
    public RegistrationTicket getByPatientDateNoon(Integer patientId, LocalDate visitDate, String noon) {
        return this.getOne(new LambdaQueryWrapper<RegistrationTicket>()
                .eq(RegistrationTicket::getPatientId, patientId)
                .eq(RegistrationTicket::getVisitDate, visitDate)
                .eq(RegistrationTicket::getNoon, noon));
    }

    @Override
    public RegistrationTicket createPending(Integer patientId, Integer employeeId, LocalDate visitDate,
                                           String noon, Integer registLevelId, Integer settleCategoryId) {
        RegistrationTicket t = new RegistrationTicket();
        t.setTicketNo(generateTicketNo());
        t.setPatientId(patientId);
        t.setEmployeeId(employeeId);
        t.setVisitDate(visitDate);
        t.setNoon(noon);
        t.setRegistLevelId(registLevelId);
        t.setSettleCategoryId(settleCategoryId);
        t.setStatus(RegistrationTicket.STATUS_PENDING);
        this.save(t);
        return t;
    }

    @Override
    public void markSuccess(Integer ticketId, Integer registerId) {
        RegistrationTicket t = new RegistrationTicket();
        t.setId(ticketId);
        t.setStatus(RegistrationTicket.STATUS_SUCCESS);
        t.setRegisterId(registerId);
        this.updateById(t);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Integer ticketId, String reason) {
        RegistrationTicket t = new RegistrationTicket();
        t.setId(ticketId);
        t.setStatus(RegistrationTicket.STATUS_FAILED);
        t.setFailReason(reason);
        this.updateById(t);
    }

    @Override
    public String generateTicketNo() {
        String ts = LocalDateTime.now().format(TS_FMT);
        int rand = ThreadLocalRandom.current().nextInt(0, 1000); // 000-999
        return "QH" + ts + String.format("%03d", rand);
    }
}
