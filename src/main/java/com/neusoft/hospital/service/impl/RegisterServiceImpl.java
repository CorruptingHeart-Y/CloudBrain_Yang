package com.neusoft.hospital.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neusoft.hospital.entity.Register;
import com.neusoft.hospital.mapper.RegisterMapper;
import com.neusoft.hospital.service.RegisterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RegisterServiceImpl extends ServiceImpl<RegisterMapper, Register> implements RegisterService {

    @Override
    public IPage<Register> pageQuery(Page<Register> page, String caseNumber, String realName, Integer visitState, LocalDate visitDateStart, LocalDate visitDateEnd, Integer deptmentId, Integer scopeEmployeeId) {
        LambdaQueryWrapper<Register> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(caseNumber)) {
            wrapper.like(Register::getCaseNumber, caseNumber);
        }
        if (StringUtils.hasText(realName)) {
            wrapper.like(Register::getRealName, realName);
        }
        if (visitState != null) {
            wrapper.eq(Register::getVisitState, visitState);
        }
        if (visitDateStart != null) {
            wrapper.ge(Register::getVisitDate, visitDateStart);
        }
        if (visitDateEnd != null) {
            wrapper.le(Register::getVisitDate, visitDateEnd);
        }
        if (deptmentId != null) {
            wrapper.eq(Register::getDeptmentId, deptmentId);
        }
        // PR4 医生范围：DOCTOR 只能看本人接诊的挂号；scopeEmployeeId 由服务端 CurrentUser 注入，非前端入参
        if (scopeEmployeeId != null) {
            wrapper.eq(Register::getEmployeeId, scopeEmployeeId);
        }
        wrapper.orderByDesc(Register::getId);
        return this.page(page, wrapper);
    }

    @Override
    public Register getByCaseNumber(String caseNumber) {
        LambdaQueryWrapper<Register> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Register::getCaseNumber, caseNumber);
        return this.getOne(wrapper);
    }

    @Override
    public boolean updateVisitState(Integer id, Integer visitState) {
        Register register = this.getById(id);
        if (register == null) {
            return false;
        }
        register.setVisitState(visitState);
        return this.updateById(register);
    }
}
