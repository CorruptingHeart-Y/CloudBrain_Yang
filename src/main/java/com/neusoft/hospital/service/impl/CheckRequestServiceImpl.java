package com.neusoft.hospital.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neusoft.hospital.entity.CheckRequest;
import com.neusoft.hospital.mapper.CheckRequestMapper;
import com.neusoft.hospital.service.CheckRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CheckRequestServiceImpl extends ServiceImpl<CheckRequestMapper, CheckRequest> implements CheckRequestService {

    @Override
    public IPage<CheckRequest> pageQuery(Page<CheckRequest> page, Integer registerId, String checkState, LocalDateTime creationTimeStart, LocalDateTime creationTimeEnd) {
        LambdaQueryWrapper<CheckRequest> wrapper = new LambdaQueryWrapper<>();
        if (registerId != null) {
            wrapper.eq(CheckRequest::getRegisterId, registerId);
        }
        if (StringUtils.hasText(checkState)) {
            wrapper.eq(CheckRequest::getCheckState, checkState);
        }
        if (creationTimeStart != null) {
            wrapper.ge(CheckRequest::getCreationTime, creationTimeStart);
        }
        if (creationTimeEnd != null) {
            wrapper.le(CheckRequest::getCreationTime, creationTimeEnd);
        }
        wrapper.orderByDesc(CheckRequest::getId);
        return this.page(page, wrapper);
    }

    @Override
    public List<CheckRequest> listByRegisterId(Integer registerId) {
        LambdaQueryWrapper<CheckRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CheckRequest::getRegisterId, registerId);
        wrapper.orderByAsc(CheckRequest::getId);
        return this.list(wrapper);
    }

    @Override
    public boolean updateState(Integer id, String state) {
        CheckRequest checkRequest = this.getById(id);
        if (checkRequest == null) {
            return false;
        }
        checkRequest.setCheckState(state);
        return this.updateById(checkRequest);
    }
}
