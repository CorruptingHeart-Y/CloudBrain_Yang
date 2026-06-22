package com.neusoft.hospital.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neusoft.hospital.entity.RegistLevel;
import com.neusoft.hospital.mapper.RegistLevelMapper;
import com.neusoft.hospital.service.RegistLevelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RegistLevelServiceImpl extends ServiceImpl<RegistLevelMapper, RegistLevel> implements RegistLevelService {

    @Override
    public IPage<RegistLevel> pageQuery(Page<RegistLevel> page) {
        LambdaQueryWrapper<RegistLevel> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(RegistLevel::getId);
        return this.page(page, wrapper);
    }

    @Override
    public List<RegistLevel> listAll() {
        LambdaQueryWrapper<RegistLevel> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(RegistLevel::getId);
        return this.list(wrapper);
    }
}
