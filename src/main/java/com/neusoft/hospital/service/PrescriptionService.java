package com.neusoft.hospital.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neusoft.hospital.entity.Prescription;

import java.util.List;

public interface PrescriptionService extends IService<Prescription> {

    IPage<Prescription> pageQuery(Page<Prescription> page, Integer registerId, String drugState, Integer scopeEmployeeId);

    List<Prescription> listByRegisterId(Integer registerId);
}
