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
    public IPage<Register> pageQuery(Page<Register> page, String caseNumber, String realName, Integer visitState, LocalDate visitDateStart, LocalDate visitDateEnd, Integer deptmentId, Integer scopeEmployeeId, List<Integer> scopeRegisterIds) {
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
        // DOCTOR 范围：scopeEmployeeId 由 CurrentUser 注入，非前端入参
        if (scopeEmployeeId != null) {
            wrapper.eq(Register::getEmployeeId, scopeEmployeeId);
        }
        // PATIENT 范围：仅返回桥接表内 link 到本人的 register；空 list 表示无 link → 返回空页
        if (scopeRegisterIds != null) {
            if (scopeRegisterIds.isEmpty()) {
                wrapper.eq(Register::getId, -1); // 永假条件，保证空结果
            } else {
                wrapper.in(Register::getId, scopeRegisterIds);
            }
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
