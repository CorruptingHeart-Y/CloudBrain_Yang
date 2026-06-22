package com.neusoft.hospital.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.hospital.common.PageResult;
import com.neusoft.hospital.common.Result;
import com.neusoft.hospital.dto.request.DisposalRequestCreateRequest;
import com.neusoft.hospital.dto.request.DisposalRequestUpdateRequest;
import com.neusoft.hospital.dto.response.DisposalRequestResponse;
import com.neusoft.hospital.entity.DisposalRequest;
import com.neusoft.hospital.entity.MedicalTechnology;
import com.neusoft.hospital.service.DisposalRequestService;
import com.neusoft.hospital.service.MedicalTechnologyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "处置申请管理", description = "处置(治疗)申请的开立、结果录入与状态流转接口")
@RestController
@RequestMapping("/api/v1/disposal-request")
@RequiredArgsConstructor
public class DisposalRequestController {

    private final DisposalRequestService disposalRequestService;
    private final MedicalTechnologyService medicalTechnologyService;

    @Operation(summary = "分页查询处置申请列表", description = "支持按挂号ID、处置状态和创建时间范围筛选")
    @GetMapping
    public Result<PageResult<DisposalRequestResponse>> page(
            @Parameter(description = "页码，默认1", example = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数，默认10", example = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "挂号ID", example = "1") @RequestParam(required = false) Integer registerId,
            @Parameter(description = "处置状态", example = "待处置") @RequestParam(required = false) String disposalState,
            @Parameter(description = "创建时间起始（含），格式yyyy-MM-dd HH:mm:ss", example = "2024-01-01 00:00:00") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime creationTimeStart,
            @Parameter(description = "创建时间截止（含），格式yyyy-MM-dd HH:mm:ss", example = "2024-12-31 23:59:59") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime creationTimeEnd) {
        Page<DisposalRequest> page = new Page<>(pageNum, pageSize);
        IPage<DisposalRequest> result = disposalRequestService.pageQuery(page, registerId, disposalState, creationTimeStart, creationTimeEnd);
        List<DisposalRequestResponse> responses = result.getRecords().stream().map(this::toResponse).toList();
        return Result.ok(PageResult.of(result.getTotal(), pageNum, pageSize, responses));
    }

    @Operation(summary = "根据挂号ID查询处置申请列表", description = "返回指定挂号下的所有处置申请记录")
    @GetMapping("/register/{registerId}")
    public Result<List<DisposalRequestResponse>> listByRegisterId(
            @Parameter(description = "挂号ID", example = "1", required = true) @PathVariable Integer registerId) {
        List<DisposalRequest> list = disposalRequestService.listByRegisterId(registerId);
        List<DisposalRequestResponse> responses = list.stream().map(this::toResponse).toList();
        return Result.ok(responses);
    }

    @Operation(summary = "根据ID查询处置申请详情", description = "通过处置申请ID获取完整信息，含医技项目名称")
    @GetMapping("/{id}")
    public Result<DisposalRequestResponse> getById(
            @Parameter(description = "处置申请ID", example = "1", required = true) @PathVariable Integer id) {
        DisposalRequest disposalRequest = disposalRequestService.getById(id);
        if (disposalRequest == null) {
            return Result.fail(404, "处置申请不存在");
        }
        return Result.ok(toResponse(disposalRequest));
    }

    @Operation(summary = "新增处置申请", description = "创建新的处置申请记录，挂号ID和医技项目ID为必填")
    @PostMapping
    public Result<Void> create(@RequestBody @Valid DisposalRequestCreateRequest request) {
        DisposalRequest disposalRequest = new DisposalRequest();
        BeanUtils.copyProperties(request, disposalRequest);
        disposalRequest.setCreationTime(LocalDateTime.now());
        disposalRequestService.save(disposalRequest);
        return Result.ok();
    }

    @Operation(summary = "修改处置申请", description = "根据ID更新处置申请信息，可用于结果录入和状态流转")
    @PutMapping
    public Result<Void> update(@RequestBody DisposalRequestUpdateRequest request) {
        DisposalRequest disposalRequest = new DisposalRequest();
        BeanUtils.copyProperties(request, disposalRequest);
        disposalRequestService.updateById(disposalRequest);
        return Result.ok();
    }

    private DisposalRequestResponse toResponse(DisposalRequest entity) {
        DisposalRequestResponse response = new DisposalRequestResponse();
        BeanUtils.copyProperties(entity, response);
        if (entity.getMedicalTechnologyId() != null) {
            MedicalTechnology tech = medicalTechnologyService.getById(entity.getMedicalTechnologyId());
            if (tech != null) {
                response.setTechName(tech.getTechName());
            }
        }
        return response;
    }
}
