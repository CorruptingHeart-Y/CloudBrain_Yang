package com.neusoft.hospital.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neusoft.hospital.entity.Scheduling;

import java.util.List;

public interface SchedulingService extends IService<Scheduling> {

    IPage<Scheduling> pageQuery(Page<Scheduling> page);

    List<Scheduling> listAll();
}
