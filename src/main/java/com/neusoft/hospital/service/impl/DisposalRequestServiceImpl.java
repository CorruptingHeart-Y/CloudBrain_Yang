package com.neusoft.hospital.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neusoft.hospital.entity.DisposalRequest;
import com.neusoft.hospital.mapper.DisposalRequestMapper;
import com.neusoft.hospital.service.DisposalRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DisposalRequestServiceImpl extends ServiceImpl<DisposalRequestMapper, DisposalRequest> implements DisposalRequestService {

    @Override
    public IPage<DisposalRequest> pageQuery(Page<DisposalRequest> page, Integer registerId, String disposalState, LocalDateTime creationTimeStart, LocalDateTime creationTimeEnd, Integer scopeEmployeeId) {
        LambdaQueryWrapper<DisposalRequest> wrapper = new LambdaQueryWrapper<>();
        if (registerId != null) {
            wrapper.eq(DisposalRequest::getRegisterId, registerId);
        }
        if (StringUtils.hasText(disposalState)) {
            wrapper.eq(DisposalRequest::getDisposalState, disposalState);
        }
        if (creationTimeStart != null) {
            wrapper.ge(DisposalRequest::getCreationTime, creationTimeStart);
        }
        if (creationTimeEnd != null) {
            wrapper.le(DisposalRequest::getCreationTime, creationTimeEnd);
        }
        // PR4 医生范围：DOCTOR 仅看本人接诊挂号下的处置申请。scopeEmployeeId 由 CurrentUser 注入(Integer)，非前端入参。
        if (scopeEmployeeId != null) {
            wrapper.inSql(DisposalRequest::getRegisterId,
                    "SELECT id FROM register WHERE employee_id = " + scopeEmployeeId);
        }
        wrapper.orderByDesc(DisposalRequest::getId);
        return this.page(page, wrapper);
    }

    @Override
    public List<DisposalRequest> listByRegisterId(Integer registerId) {
        LambdaQueryWrapper<DisposalRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DisposalRequest::getRegisterId, registerId);
        wrapper.orderByAsc(DisposalRequest::getId);
        return this.list(wrapper);
    }

    @Override
    public boolean updateState(Integer id, String state) {
        DisposalRequest disposalRequest = this.getById(id);
        if (disposalRequest == null) {
            return false;
        }
        disposalRequest.setDisposalState(state);
        return this.updateById(disposalRequest);
    }
}
