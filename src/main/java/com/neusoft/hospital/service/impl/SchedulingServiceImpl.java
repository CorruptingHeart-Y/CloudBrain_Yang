package com.neusoft.hospital.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neusoft.hospital.entity.Scheduling;
import com.neusoft.hospital.mapper.SchedulingMapper;
import com.neusoft.hospital.service.SchedulingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SchedulingServiceImpl extends ServiceImpl<SchedulingMapper, Scheduling> implements SchedulingService {

    @Override
    public IPage<Scheduling> pageQuery(Page<Scheduling> page) {
        LambdaQueryWrapper<Scheduling> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Scheduling::getId);
        return this.page(page, wrapper);
    }

    @Override
    public List<Scheduling> listAll() {
        LambdaQueryWrapper<Scheduling> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Scheduling::getId);
        return this.list(wrapper);
    }
}
