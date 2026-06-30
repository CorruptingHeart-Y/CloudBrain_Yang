package com.neusoft.hospital.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.hospital.auth.annotation.RequireRole;
import com.neusoft.hospital.auth.enums.Role;
import com.neusoft.hospital.common.BusinessException;
import com.neusoft.hospital.common.ErrorCode;
import com.neusoft.hospital.common.PageResult;
import com.neusoft.hospital.common.Result;
import com.neusoft.hospital.dto.request.PrescriptionCreateRequest;
import com.neusoft.hospital.dto.request.PrescriptionUpdateRequest;
import com.neusoft.hospital.dto.response.PrescriptionResponse;
import com.neusoft.hospital.entity.DrugInfo;
import com.neusoft.hospital.entity.Prescription;
import com.neusoft.hospital.service.DrugInfoService;
import com.neusoft.hospital.service.PrescriptionService;
import com.neusoft.hospital.service.RegisterOwnership;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "处方管理", description = "门诊处方开立与状态管理接口；DOCTOR 仅限本人接诊挂号")
@RestController
@RequestMapping("/api/v1/prescription")
@RequiredArgsConstructor
@RequireRole({Role.ADMIN, Role.DOCTOR})
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final DrugInfoService drugInfoService;
    private final RegisterOwnership registerOwnership;

    @Operation(summary = "分页查询处方列表", description = "支持按挂号ID和药品状态筛选；DOCTOR 仅返回本人接诊挂号下的处方")
    @GetMapping
    public Result<PageResult<PrescriptionResponse>> page(
            @Parameter(description = "页码，默认1", example = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数，默认10", example = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "挂号ID", example = "1") @RequestParam(required = false) Integer registerId,
            @Parameter(description = "药品状态", example = "待发药") @RequestParam(required = false) String drugState) {
        Page<Prescription> page = new Page<>(pageNum, pageSize);
        IPage<Prescription> result = prescriptionService.pageQuery(page, registerId, drugState, registerOwnership.doctorScopeEmployeeIdOrNull());
        List<PrescriptionResponse> responses = result.getRecords().stream().map(this::toResponse).toList();
        return Result.ok(PageResult.of(result.getTotal(), pageNum, pageSize, responses));
    }

    @Operation(summary = "根据挂号ID查询处方列表", description = "返回指定挂号下的所有处方记录；DOCTOR 仅限本人接诊，他人/不存在 → 404")
    @GetMapping("/register/{registerId}")
    public Result<List<PrescriptionResponse>> listByRegisterId(
            @Parameter(description = "挂号ID", example = "1", required = true) @PathVariable Integer registerId) {
        // 先 SQL 校验挂号归属，再读取派生数据；DOCTOR 非本人 → 404
        registerOwnership.requireAccessibleRegister(registerId);
        List<Prescription> list = prescriptionService.listByRegisterId(registerId);
        List<PrescriptionResponse> responses = list.stream().map(this::toResponse).toList();
        return Result.ok(responses);
    }

    @Operation(summary = "根据ID查询处方详情", description = "通过处方ID获取完整信息；DOCTOR 仅限本人接诊挂号，他人/不存在 → 404")
    @GetMapping("/{id}")
    public Result<PrescriptionResponse> getById(
            @Parameter(description = "处方ID", example = "1", required = true) @PathVariable Integer id) {
        Prescription prescription = prescriptionService.getById(id);
        if (prescription == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        registerOwnership.requireAccessibleRegister(prescription.getRegisterId());
        return Result.ok(toResponse(prescription));
    }

    @Operation(summary = "新增处方", description = "创建新的处方记录；registerId 必须属于当前医生(DOCTOR)，他人/不存在 → 404")
    @PostMapping
    public Result<Void> create(@RequestBody @Valid PrescriptionCreateRequest request) {
        // 先校验挂号归属，防止为他人接诊的挂号开方
        registerOwnership.requireAccessibleRegister(request.getRegisterId());
        Prescription prescription = new Prescription();
        BeanUtils.copyProperties(request, prescription);
        prescription.setCreationTime(LocalDateTime.now());
        prescriptionService.save(prescription);
        return Result.ok();
    }

    @Operation(summary = "修改处方", description = "根据ID更新处方信息；DOCTOR 仅限本人接诊挂号的处方，他人/不存在 → 404")
    @PutMapping
    public Result<Void> update(@RequestBody PrescriptionUpdateRequest request) {
        Prescription existing = prescriptionService.getById(request.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        // 归属判定下沉到 SQL（register.employee_id），不信任 body 中的任何 employeeId 字段
        registerOwnership.requireAccessibleRegister(existing.getRegisterId());
        Prescription prescription = new Prescription();
        BeanUtils.copyProperties(request, prescription);
        prescriptionService.updateById(prescription);
        return Result.ok();
    }

    private PrescriptionResponse toResponse(Prescription entity) {
        PrescriptionResponse response = new PrescriptionResponse();
        BeanUtils.copyProperties(entity, response);
        if (entity.getDrugId() != null) {
            DrugInfo drugInfo = drugInfoService.getById(entity.getDrugId());
            if (drugInfo != null) {
                response.setDrugName(drugInfo.getDrugName());
                response.setDrugFormat(drugInfo.getDrugFormat());
            }
        }
        return response;
    }
}
