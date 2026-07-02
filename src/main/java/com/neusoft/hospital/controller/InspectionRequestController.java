package com.neusoft.hospital.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.hospital.auth.annotation.RequireRole;
import com.neusoft.hospital.auth.enums.Role;
import com.neusoft.hospital.common.BusinessException;
import com.neusoft.hospital.common.ErrorCode;
import com.neusoft.hospital.common.PageResult;
import com.neusoft.hospital.common.Result;
import com.neusoft.hospital.dto.request.InspectionRequestCreateRequest;
import com.neusoft.hospital.dto.request.InspectionRequestUpdateRequest;
import com.neusoft.hospital.dto.response.InspectionRequestResponse;
import com.neusoft.hospital.entity.InspectionRequest;
import com.neusoft.hospital.entity.MedicalTechnology;
import com.neusoft.hospital.service.InspectionRequestService;
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

@Tag(name = "检验申请管理", description = "检验(化验)申请的开立、结果录入与状态流转接口；DOCTOR 仅限本人接诊挂号")
@RestController
@RequestMapping("/api/v1/inspection-request")
@RequiredArgsConstructor
@RequireRole({Role.ADMIN, Role.DOCTOR})
@SecurityRequirement(name = "Bearer")
public class InspectionRequestController {

    private final InspectionRequestService inspectionRequestService;
    private final MedicalTechnologyService medicalTechnologyService;
    private final RegisterOwnership registerOwnership;

    @Operation(summary = "分页查询检验申请列表", description = "支持按挂号ID、检验状态和创建时间范围筛选；DOCTOR 仅返回本人接诊挂号下的记录")
    @GetMapping
    public Result<PageResult<InspectionRequestResponse>> page(
            @Parameter(description = "页码，默认1", example = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数，默认10", example = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "挂号ID", example = "1") @RequestParam(required = false) Integer registerId,
            @Parameter(description = "检验状态", example = "待检验") @RequestParam(required = false) String inspectionState,
            @Parameter(description = "创建时间起始（含），格式yyyy-MM-dd HH:mm:ss", example = "2024-01-01 00:00:00") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime creationTimeStart,
            @Parameter(description = "创建时间截止（含），格式yyyy-MM-dd HH:mm:ss", example = "2024-12-31 23:59:59") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime creationTimeEnd) {
        Page<InspectionRequest> page = new Page<>(pageNum, pageSize);
        IPage<InspectionRequest> result = inspectionRequestService.pageQuery(page, registerId, inspectionState, creationTimeStart, creationTimeEnd, registerOwnership.doctorScopeEmployeeIdOrNull());
        List<InspectionRequestResponse> responses = result.getRecords().stream().map(this::toResponse).toList();
        return Result.ok(PageResult.of(result.getTotal(), pageNum, pageSize, responses));
    }

    @Operation(summary = "根据挂号ID查询检验申请列表", description = "返回指定挂号下的所有检验申请记录；DOCTOR 仅限本人接诊，他人/不存在 → 404")
    @GetMapping("/register/{registerId}")
    public Result<List<InspectionRequestResponse>> listByRegisterId(
            @Parameter(description = "挂号ID", example = "1", required = true) @PathVariable Integer registerId) {
        registerOwnership.requireAccessibleRegister(registerId);
        List<InspectionRequest> list = inspectionRequestService.listByRegisterId(registerId);
        List<InspectionRequestResponse> responses = list.stream().map(this::toResponse).toList();
        return Result.ok(responses);
    }

    @Operation(summary = "根据ID查询检验申请详情", description = "通过检验申请ID获取完整信息；DOCTOR 仅限本人接诊挂号，他人/不存在 → 404")
    @GetMapping("/{id}")
    public Result<InspectionRequestResponse> getById(
            @Parameter(description = "检验申请ID", example = "1", required = true) @PathVariable Integer id) {
        InspectionRequest inspectionRequest = inspectionRequestService.getById(id);
        if (inspectionRequest == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        registerOwnership.requireAccessibleRegister(inspectionRequest.getRegisterId());
        return Result.ok(toResponse(inspectionRequest));
    }

    @Operation(summary = "新增检验申请", description = "创建新的检验申请记录；registerId 必须属于当前医生(DOCTOR)，他人/不存在 → 404")
    @PostMapping
    public Result<Void> create(@RequestBody @Valid InspectionRequestCreateRequest request) {
        registerOwnership.requireAccessibleRegister(request.getRegisterId());
        InspectionRequest inspectionRequest = new InspectionRequest();
        BeanUtils.copyProperties(request, inspectionRequest);
        inspectionRequest.setCreationTime(LocalDateTime.now());
        inspectionRequestService.save(inspectionRequest);
        return Result.ok();
    }

    @Operation(summary = "修改检验申请", description = "根据ID更新检验申请信息；DOCTOR 仅限本人接诊挂号的记录，他人/不存在 → 404")
    @PutMapping
    public Result<Void> update(@RequestBody InspectionRequestUpdateRequest request) {
        InspectionRequest existing = inspectionRequestService.getById(request.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        // 归属判定下沉到 SQL（register.employee_id），body 中的 inspectionEmployeeId 等为业务字段，不影响归属
        registerOwnership.requireAccessibleRegister(existing.getRegisterId());
        InspectionRequest inspectionRequest = new InspectionRequest();
        BeanUtils.copyProperties(request, inspectionRequest);
        inspectionRequestService.updateById(inspectionRequest);
        return Result.ok();
    }

    private InspectionRequestResponse toResponse(InspectionRequest entity) {
        InspectionRequestResponse response = new InspectionRequestResponse();
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
