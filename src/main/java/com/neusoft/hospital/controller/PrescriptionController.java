package com.neusoft.hospital.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.hospital.common.PageResult;
import com.neusoft.hospital.common.Result;
import com.neusoft.hospital.dto.request.PrescriptionCreateRequest;
import com.neusoft.hospital.dto.request.PrescriptionUpdateRequest;
import com.neusoft.hospital.dto.response.PrescriptionResponse;
import com.neusoft.hospital.entity.DrugInfo;
import com.neusoft.hospital.entity.Prescription;
import com.neusoft.hospital.service.DrugInfoService;
import com.neusoft.hospital.service.PrescriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "处方管理", description = "门诊处方开立与状态管理接口")
@RestController
@RequestMapping("/api/v1/prescription")
@RequiredArgsConstructor
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final DrugInfoService drugInfoService;

    @Operation(summary = "分页查询处方列表", description = "支持按挂号ID和药品状态筛选")
    @GetMapping
    public Result<PageResult<PrescriptionResponse>> page(
            @Parameter(description = "页码，默认1", example = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数，默认10", example = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "挂号ID", example = "1") @RequestParam(required = false) Integer registerId,
            @Parameter(description = "药品状态", example = "待发药") @RequestParam(required = false) String drugState) {
        Page<Prescription> page = new Page<>(pageNum, pageSize);
        IPage<Prescription> result = prescriptionService.pageQuery(page, registerId, drugState);
        List<PrescriptionResponse> responses = result.getRecords().stream().map(this::toResponse).toList();
        return Result.ok(PageResult.of(result.getTotal(), pageNum, pageSize, responses));
    }

    @Operation(summary = "根据挂号ID查询处方列表", description = "返回指定挂号下的所有处方记录")
    @GetMapping("/register/{registerId}")
    public Result<List<PrescriptionResponse>> listByRegisterId(
            @Parameter(description = "挂号ID", example = "1", required = true) @PathVariable Integer registerId) {
        List<Prescription> list = prescriptionService.listByRegisterId(registerId);
        List<PrescriptionResponse> responses = list.stream().map(this::toResponse).toList();
        return Result.ok(responses);
    }

    @Operation(summary = "根据ID查询处方详情", description = "通过处方ID获取完整信息，含药品名称和药品规格")
    @GetMapping("/{id}")
    public Result<PrescriptionResponse> getById(
            @Parameter(description = "处方ID", example = "1", required = true) @PathVariable Integer id) {
        Prescription prescription = prescriptionService.getById(id);
        if (prescription == null) {
            return Result.fail(404, "处方不存在");
        }
        return Result.ok(toResponse(prescription));
    }

    @Operation(summary = "新增处方", description = "创建新的处方记录，挂号ID和药品ID为必填")
    @PostMapping
    public Result<Void> create(@RequestBody @Valid PrescriptionCreateRequest request) {
        Prescription prescription = new Prescription();
        BeanUtils.copyProperties(request, prescription);
        prescription.setCreationTime(LocalDateTime.now());
        prescriptionService.save(prescription);
        return Result.ok();
    }

    @Operation(summary = "修改处方", description = "根据ID更新处方信息，可用于状态管理")
    @PutMapping
    public Result<Void> update(@RequestBody PrescriptionUpdateRequest request) {
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
