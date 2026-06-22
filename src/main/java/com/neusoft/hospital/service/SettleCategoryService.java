package com.neusoft.hospital.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neusoft.hospital.entity.SettleCategory;

import java.util.List;

public interface SettleCategoryService extends IService<SettleCategory> {

    IPage<SettleCategory> pageQuery(Page<SettleCategory> page);

    List<SettleCategory> listAll();
}
