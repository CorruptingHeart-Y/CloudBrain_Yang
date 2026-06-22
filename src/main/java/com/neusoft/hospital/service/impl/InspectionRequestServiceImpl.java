package com.neusoft.hospital.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neusoft.hospital.entity.InspectionRequest;
import com.neusoft.hospital.mapper.InspectionRequestMapper;
import com.neusoft.hospital.service.InspectionRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InspectionRequestServiceImpl extends ServiceImpl<InspectionRequestMapper, InspectionRequest> implements InspectionRequestService {

    @Override
    public IPage<InspectionRequest> pageQuery(Page<InspectionRequest> page, Integer registerId, String inspectionState, LocalDateTime creationTimeStart, LocalDateTime creationTimeEnd) {
        LambdaQueryWrapper<InspectionRequest> wrapper = new LambdaQueryWrapper<>();
        if (registerId != null) {
            wrapper.eq(InspectionRequest::getRegisterId, registerId);
        }
        if (StringUtils.hasText(inspectionState)) {
            wrapper.eq(InspectionRequest::getInspectionState, inspectionState);
        }
        if (creationTimeStart != null) {
            wrapper.ge(InspectionRequest::getCreationTime, creationTimeStart);
        }
        if (creationTimeEnd != null) {
            wrapper.le(InspectionRequest::getCreationTime, creationTimeEnd);
        }
        wrapper.orderByDesc(InspectionRequest::getId);
        return this.page(page, wrapper);
    }

    @Override
    public List<InspectionRequest> listByRegisterId(Integer registerId) {
        LambdaQueryWrapper<InspectionRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InspectionRequest::getRegisterId, registerId);
        wrapper.orderByAsc(InspectionRequest::getId);
        return this.list(wrapper);
    }

    @Override
    public boolean updateState(Integer id, String state) {
        InspectionRequest inspectionRequest = this.getById(id);
        if (inspectionRequest == null) {
            return false;
        }
        inspectionRequest.setInspectionState(state);
        return this.updateById(inspectionRequest);
    }
}
