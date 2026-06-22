package com.neusoft.hospital.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neusoft.hospital.entity.Disease;
import com.neusoft.hospital.mapper.DiseaseMapper;
import com.neusoft.hospital.service.DiseaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DiseaseServiceImpl extends ServiceImpl<DiseaseMapper, Disease> implements DiseaseService {

    @Override
    public IPage<Disease> pageQuery(Page<Disease> page, String keyword) {
        LambdaQueryWrapper<Disease> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Disease::getDiseaseCode, keyword)
                   .or()
                   .like(Disease::getDiseaseName, keyword);
        }
        wrapper.orderByAsc(Disease::getId);
        return this.page(page, wrapper);
    }

    @Override
    public List<Disease> searchByKeyword(String keyword) {
        LambdaQueryWrapper<Disease> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Disease::getDiseaseCode, keyword)
                   .or()
                   .like(Disease::getDiseaseName, keyword);
        }
        wrapper.orderByAsc(Disease::getId);
        return this.list(wrapper);
    }
}
