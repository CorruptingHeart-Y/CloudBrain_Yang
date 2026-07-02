package com.neusoft.hospital.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.hospital.auth.annotation.RequireRole;
import com.neusoft.hospital.auth.enums.Role;
import com.neusoft.hospital.common.BusinessException;
import com.neusoft.hospital.common.ErrorCode;
import com.neusoft.hospital.common.PageResult;
import com.neusoft.hospital.common.Result;
import com.neusoft.hospital.dto.request.DisposalRequestCreateRequest;
import com.neusoft.hospital.dto.request.DisposalRequestUpdateRequest;
import com.neusoft.hospital.dto.response.DisposalRequestResponse;
import com.neusoft.hospital.entity.DisposalRequest;
import com.neusoft.hospital.entity.MedicalTechnology;
import com.neusoft.hospital.service.DisposalRequestService;
import com.neusoft.hospital.service.MedicalTechnologyService;
import com.neusoft.hospital.service.RegisterOwnership;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "处置申请管理", description = "处置(治疗)申请的开立、结果录入与状态流转接口；DOCTOR 仅限本人接诊挂号")
@RestController
@RequestMapping("/api/v1/disposal-request")
@RequiredArgsConstructor
@RequireRole({Role.ADMIN, Role.DOCTOR})
@SecurityRequirement(name = "Bearer")
public class DisposalRequestController {

    private final DisposalRequestService disposalRequestService;
    private final MedicalTechnologyService medicalTechnologyService;
    private final RegisterOwnership registerOwnership;

    @Operation(summary = "分页查询处置申请列表", description = "支持按挂号ID、处置状态和创建时间范围筛选；DOCTOR 仅返回本人接诊挂号下的记录")
    @GetMapping
    public Result<PageResult<DisposalRequestResponse>> page(
            @Parameter(description = "页码，默认1", example = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数，默认10", example = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "挂号ID", example = "1") @RequestParam(required = false) Integer registerId,
            @Parameter(description = "处置状态", example = "待处置") @RequestParam(required = false) String disposalState,
            @Parameter(description = "创建时间起始（含），格式yyyy-MM-dd HH:mm:ss", example = "2024-01-01 00:00:00") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime creationTimeStart,
            @Parameter(description = "创建时间截止（含），格式yyyy-MM-dd HH:mm:ss", example = "2024-12-31 23:59:59") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime creationTimeEnd) {
        Page<DisposalRequest> page = new Page<>(pageNum, pageSize);
        IPage<DisposalRequest> result = disposalRequestService.pageQuery(page, registerId, disposalState, creationTimeStart, creationTimeEnd, registerOwnership.doctorScopeEmployeeIdOrNull());
        List<DisposalRequestResponse> responses = result.getRecords().stream().map(this::toResponse).toList();
        return Result.ok(PageResult.of(result.getTotal(), pageNum, pageSize, responses));
    }

    @Operation(summary = "根据挂号ID查询处置申请列表", description = "返回指定挂号下的所有处置申请记录；DOCTOR 仅限本人接诊，他人/不存在 → 404")
    @GetMapping("/register/{registerId}")
    public Result<List<DisposalRequestResponse>> listByRegisterId(
            @Parameter(description = "挂号ID", example = "1", required = true) @PathVariable Integer registerId) {
        registerOwnership.requireAccessibleRegister(registerId);
        List<DisposalRequest> list = disposalRequestService.listByRegisterId(registerId);
        List<DisposalRequestResponse> responses = list.stream().map(this::toResponse).toList();
        return Result.ok(responses);
    }

    @Operation(summary = "根据ID查询处置申请详情", description = "通过处置申请ID获取完整信息；DOCTOR 仅限本人接诊挂号，他人/不存在 → 404")
    @GetMapping("/{id}")
    public Result<DisposalRequestResponse> getById(
            @Parameter(description = "处置申请ID", example = "1", required = true) @PathVariable Integer id) {
        DisposalRequest disposalRequest = disposalRequestService.getById(id);
        if (disposalRequest == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        registerOwnership.requireAccessibleRegister(disposalRequest.getRegisterId());
        return Result.ok(toResponse(disposalRequest));
    }

    @Operation(summary = "新增处置申请", description = "创建新的处置申请记录；registerId 必须属于当前医生(DOCTOR)，他人/不存在 → 404")
    @PostMapping
    public Result<Void> create(@RequestBody @Valid DisposalRequestCreateRequest request) {
        registerOwnership.requireAccessibleRegister(request.getRegisterId());
        DisposalRequest disposalRequest = new DisposalRequest();
        BeanUtils.copyProperties(request, disposalRequest);
        disposalRequest.setCreationTime(LocalDateTime.now());
        disposalRequestService.save(disposalRequest);
        return Result.ok();
    }

    @Operation(summary = "修改处置申请", description = "根据ID更新处置申请信息；DOCTOR 仅限本人接诊挂号的记录，他人/不存在 → 404")
    @PutMapping
    public Result<Void> update(@RequestBody DisposalRequestUpdateRequest request) {
        DisposalRequest existing = disposalRequestService.getById(request.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        // 归属判定下沉到 SQL（register.employee_id），body 中的 disposalEmployeeId 等为业务字段，不影响归属
        registerOwnership.requireAccessibleRegister(existing.getRegisterId());
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
