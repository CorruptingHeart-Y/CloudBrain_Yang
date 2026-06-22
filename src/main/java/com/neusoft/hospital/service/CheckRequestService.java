package com.neusoft.hospital.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neusoft.hospital.entity.CheckRequest;

import java.time.LocalDateTime;
import java.util.List;

public interface CheckRequestService extends IService<CheckRequest> {

    IPage<CheckRequest> pageQuery(Page<CheckRequest> page, Integer registerId, String checkState, LocalDateTime creationTimeStart, LocalDateTime creationTimeEnd);

    List<CheckRequest> listByRegisterId(Integer registerId);

    boolean updateState(Integer id, String state);
}
