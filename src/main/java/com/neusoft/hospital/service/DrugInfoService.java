package com.neusoft.hospital.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neusoft.hospital.entity.DrugInfo;

import java.util.List;

public interface DrugInfoService extends IService<DrugInfo> {

    IPage<DrugInfo> pageQuery(Page<DrugInfo> page, String keyword, String drugType);

    List<DrugInfo> searchByKeyword(String keyword);
}
