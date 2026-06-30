package com.neusoft.hospital.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neusoft.hospital.entity.Register;

import java.time.LocalDate;
import java.util.List;

public interface RegisterService extends IService<Register> {

    IPage<Register> pageQuery(Page<Register> page, String caseNumber, String realName, Integer visitState, LocalDate visitDateStart, LocalDate visitDateEnd, Integer deptmentId, Integer scopeEmployeeId);

    Register getByCaseNumber(String caseNumber);

    boolean updateVisitState(Integer id, Integer visitState);
}
