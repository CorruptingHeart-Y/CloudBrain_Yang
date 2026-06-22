package com.neusoft.hospital.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neusoft.hospital.entity.RegistLevel;

import java.util.List;

public interface RegistLevelService extends IService<RegistLevel> {

    IPage<RegistLevel> pageQuery(Page<RegistLevel> page);

    List<RegistLevel> listAll();
}
