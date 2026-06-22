package com.neusoft.hospital.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neusoft.hospital.entity.Disease;

import java.util.List;

public interface DiseaseService extends IService<Disease> {

    IPage<Disease> pageQuery(Page<Disease> page, String keyword);

    List<Disease> searchByKeyword(String keyword);
}
