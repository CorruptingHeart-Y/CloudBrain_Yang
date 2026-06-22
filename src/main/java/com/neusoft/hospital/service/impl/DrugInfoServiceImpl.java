package com.neusoft.hospital.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neusoft.hospital.entity.DrugInfo;
import com.neusoft.hospital.mapper.DrugInfoMapper;
import com.neusoft.hospital.service.DrugInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DrugInfoServiceImpl extends ServiceImpl<DrugInfoMapper, DrugInfo> implements DrugInfoService {

    @Override
    public IPage<DrugInfo> pageQuery(Page<DrugInfo> page, String keyword, String drugType) {
        LambdaQueryWrapper<DrugInfo> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(DrugInfo::getMnemonicCode, keyword)
                   .or()
                   .like(DrugInfo::getDrugName, keyword);
        }
        if (StringUtils.hasText(drugType)) {
            wrapper.eq(DrugInfo::getDrugType, drugType);
        }
        wrapper.orderByAsc(DrugInfo::getId);
        return this.page(page, wrapper);
    }

    @Override
    public List<DrugInfo> searchByKeyword(String keyword) {
        LambdaQueryWrapper<DrugInfo> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(DrugInfo::getMnemonicCode, keyword)
                   .or()
                   .like(DrugInfo::getDrugName, keyword);
        }
        wrapper.orderByAsc(DrugInfo::getId);
        return this.list(wrapper);
    }
}
