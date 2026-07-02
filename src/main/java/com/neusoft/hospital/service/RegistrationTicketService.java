package com.neusoft.hospital.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.neusoft.hospital.entity.RegistrationTicket;

import java.time.LocalDate;

public interface RegistrationTicketService extends IService<RegistrationTicket> {

    RegistrationTicket getByTicketNo(String ticketNo);

    RegistrationTicket getByPatientDateNoon(Integer patientId, LocalDate visitDate, String noon);

    RegistrationTicket createPending(Integer patientId, Integer employeeId, LocalDate visitDate,
                                     String noon, Integer registLevelId, Integer settleCategoryId);

    void markSuccess(Integer ticketId, Integer registerId);

    void markFailed(Integer ticketId, String reason);

    String generateTicketNo();
}
