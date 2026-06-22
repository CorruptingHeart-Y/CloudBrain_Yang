package com.neusoft.hospital.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neusoft.hospital.entity.MedicalTechnology;
import com.neusoft.hospital.mapper.MedicalTechnologyMapper;
import com.neusoft.hospital.service.MedicalTechnologyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MedicalTechnologyServiceImpl extends ServiceImpl<MedicalTechnologyMapper, MedicalTechnology> implements MedicalTechnologyService {

    @Override
    public IPage<MedicalTechnology> pageQuery(Page<MedicalTechnology> page, String techName, String techType, Integer deptmentId) {
        LambdaQueryWrapper<MedicalTechnology> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(techName)) {
            wrapper.like(MedicalTechnology::getTechName, techName);
        }
        if (StringUtils.hasText(techType)) {
            wrapper.eq(MedicalTechnology::getTechType, techType);
        }
        if (deptmentId != null) {
            wrapper.eq(MedicalTechnology::getDeptmentId, deptmentId);
        }
        wrapper.orderByAsc(MedicalTechnology::getId);
        return this.page(page, wrapper);
    }

    @Override
    public List<MedicalTechnology> listAll() {
        LambdaQueryWrapper<MedicalTechnology> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(MedicalTechnology::getId);
        return this.list(wrapper);
    }
}
