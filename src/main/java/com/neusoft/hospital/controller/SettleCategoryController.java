package com.neusoft.hospital.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.hospital.common.PageResult;
import com.neusoft.hospital.common.Result;
import com.neusoft.hospital.dto.request.SettleCategoryCreateRequest;
import com.neusoft.hospital.dto.request.SettleCategoryUpdateRequest;
import com.neusoft.hospital.dto.response.SettleCategoryResponse;
import com.neusoft.hospital.entity.SettleCategory;
import com.neusoft.hospital.service.SettleCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "结算类别管理", description = "结算类别字典的增删改查接口")
@RestController
@RequestMapping("/api/v1/settle-category")
@RequiredArgsConstructor
public class SettleCategoryController {

    private final SettleCategoryService settleCategoryService;

    @Operation(summary = "分页查询结算类别列表", description = "按ID升序排列")
    @GetMapping
    public Result<PageResult<SettleCategoryResponse>> page(
            @Parameter(description = "页码，默认1", example = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数，默认10", example = "10") @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<SettleCategory> page = new Page<>(pageNum, pageSize);
        IPage<SettleCategory> result = settleCategoryService.pageQuery(page);
        List<SettleCategoryResponse> responses = result.getRecords().stream().map(this::toResponse).toList();
        return Result.ok(PageResult.of(result.getTotal(), pageNum, pageSize, responses));
    }

    @Operation(summary = "查询全部结算类别(不分页)", description = "返回所有有效结算类别列表，用于下拉选择等场景")
    @GetMapping("/all")
    public Result<List<SettleCategoryResponse>> listAll() {
        List<SettleCategory> list = settleCategoryService.listAll();
        List<SettleCategoryResponse> responses = list.stream().map(this::toResponse).toList();
        return Result.ok(responses);
    }

    @Operation(summary = "根据ID查询结算类别详情", description = "通过结算类别ID获取完整信息")
    @GetMapping("/{id}")
    public Result<SettleCategoryResponse> getById(
            @Parameter(description = "结算类别ID", example = "1", required = true) @PathVariable Integer id) {
        SettleCategory settleCategory = settleCategoryService.getById(id);
        if (settleCategory == null) {
            return Result.fail(404, "结算类别不存在");
        }
        return Result.ok(toResponse(settleCategory));
    }

    @Operation(summary = "新增结算类别", description = "创建新的结算类别记录")
    @PostMapping
    public Result<Void> create(@RequestBody @Valid SettleCategoryCreateRequest request) {
        SettleCategory settleCategory = new SettleCategory();
        BeanUtils.copyProperties(request, settleCategory);
        settleCategoryService.save(settleCategory);
        return Result.ok();
    }

    @Operation(summary = "修改结算类别", description = "根据ID更新结算类别信息")
    @PutMapping
    public Result<Void> update(@RequestBody @Valid SettleCategoryUpdateRequest request) {
        SettleCategory settleCategory = new SettleCategory();
        BeanUtils.copyProperties(request, settleCategory);
        settleCategoryService.updateById(settleCategory);
        return Result.ok();
    }

    @Operation(summary = "删除结算类别(逻辑删除)", description = "将结算类别标记为已删除，delmark从1变为0")
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @Parameter(description = "结算类别ID", example = "1", required = true) @PathVariable Integer id) {
        settleCategoryService.removeById(id);
        return Result.ok();
    }

    private SettleCategoryResponse toResponse(SettleCategory entity) {
        SettleCategoryResponse response = new SettleCategoryResponse();
        BeanUtils.copyProperties(entity, response);
        return response;
    }
}
