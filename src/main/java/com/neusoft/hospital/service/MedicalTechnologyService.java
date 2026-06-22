package com.neusoft.hospital.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neusoft.hospital.entity.MedicalTechnology;

import java.util.List;

public interface MedicalTechnologyService extends IService<MedicalTechnology> {

    IPage<MedicalTechnology> pageQuery(Page<MedicalTechnology> page, String techName, String techType, Integer deptmentId);

    List<MedicalTechnology> listAll();
}
