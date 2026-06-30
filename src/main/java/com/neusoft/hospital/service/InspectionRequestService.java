package com.neusoft.hospital.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neusoft.hospital.entity.InspectionRequest;

import java.time.LocalDateTime;
import java.util.List;

public interface InspectionRequestService extends IService<InspectionRequest> {

    IPage<InspectionRequest> pageQuery(Page<InspectionRequest> page, Integer registerId, String inspectionState, LocalDateTime creationTimeStart, LocalDateTime creationTimeEnd, Integer scopeEmployeeId);

    List<InspectionRequest> listByRegisterId(Integer registerId);

    boolean updateState(Integer id, String state);
}
