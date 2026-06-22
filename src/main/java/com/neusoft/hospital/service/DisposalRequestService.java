package com.neusoft.hospital.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neusoft.hospital.entity.DisposalRequest;

import java.time.LocalDateTime;
import java.util.List;

public interface DisposalRequestService extends IService<DisposalRequest> {

    IPage<DisposalRequest> pageQuery(Page<DisposalRequest> page, Integer registerId, String disposalState, LocalDateTime creationTimeStart, LocalDateTime creationTimeEnd);

    List<DisposalRequest> listByRegisterId(Integer registerId);

    boolean updateState(Integer id, String state);
}
