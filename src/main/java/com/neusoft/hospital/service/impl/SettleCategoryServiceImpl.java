package com.neusoft.hospital.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neusoft.hospital.entity.SettleCategory;
import com.neusoft.hospital.mapper.SettleCategoryMapper;
import com.neusoft.hospital.service.SettleCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SettleCategoryServiceImpl extends ServiceImpl<SettleCategoryMapper, SettleCategory> implements SettleCategoryService {

    @Override
    public IPage<SettleCategory> pageQuery(Page<SettleCategory> page) {
        LambdaQueryWrapper<SettleCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SettleCategory::getId);
        return this.page(page, wrapper);
    }

    @Override
    public List<SettleCategory> listAll() {
        LambdaQueryWrapper<SettleCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SettleCategory::getId);
        return this.list(wrapper);
    }
}
