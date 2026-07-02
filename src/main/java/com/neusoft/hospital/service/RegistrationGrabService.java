package com.neusoft.hospital.service;

import com.neusoft.hospital.dto.request.RegistrationGrabRequest;
import com.neusoft.hospital.dto.response.RegistrationGrabResponse;
import com.neusoft.hospital.dto.response.RegistrationTicketResponse;

public interface RegistrationGrabService {

    /** 患者抢号：幂等(一人一号) + Redis Lua 扣减 + 投递 MQ。 */
    RegistrationGrabResponse grab(RegistrationGrabRequest request);

    /** 查询抢号结果（归属当前患者）。 */
    RegistrationTicketResponse getResult(String ticketNo);
}
