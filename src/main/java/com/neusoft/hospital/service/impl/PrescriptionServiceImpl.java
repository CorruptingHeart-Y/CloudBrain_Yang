package com.neusoft.hospital.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neusoft.hospital.entity.Prescription;
import com.neusoft.hospital.mapper.PrescriptionMapper;
import com.neusoft.hospital.service.PrescriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PrescriptionServiceImpl extends ServiceImpl<PrescriptionMapper, Prescription> implements PrescriptionService {

    @Override
    public IPage<Prescription> pageQuery(Page<Prescription> page, Integer registerId, String drugState, Integer scopeEmployeeId) {
        LambdaQueryWrapper<Prescription> wrapper = new LambdaQueryWrapper<>();
        if (registerId != null) {
            wrapper.eq(Prescription::getRegisterId, registerId);
        }
        if (StringUtils.hasText(drugState)) {
            wrapper.eq(Prescription::getDrugState, drugState);
        }
        // PR4 医生范围：DOCTOR 仅看本人接诊挂号(register.employee_id=当前医生)下的处方。
        // scopeEmployeeId 由服务端 CurrentUser 注入(Integer)，非前端入参，内联子查询无注入风险。
        if (scopeEmployeeId != null) {
            wrapper.inSql(Prescription::getRegisterId,
                    "SELECT id FROM register WHERE employee_id = " + scopeEmployeeId);
        }
        wrapper.orderByDesc(Prescription::getId);
        return this.page(page, wrapper);
    }

    @Override
    public List<Prescription> listByRegisterId(Integer registerId) {
        LambdaQueryWrapper<Prescription> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Prescription::getRegisterId, registerId);
        wrapper.orderByAsc(Prescription::getId);
        return this.list(wrapper);
    }
}
