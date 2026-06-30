package com.neusoft.hospital.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.hospital.common.PageResult;
import com.neusoft.hospital.common.Result;
import com.neusoft.hospital.dto.request.DrugInfoCreateRequest;
import com.neusoft.hospital.dto.request.DrugInfoUpdateRequest;
import com.neusoft.hospital.dto.request.DrugInfoQueryRequest;
import com.neusoft.hospital.dto.response.DrugInfoResponse;
import com.neusoft.hospital.entity.DrugInfo;
import com.neusoft.hospital.service.DrugInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.neusoft.hospital.auth.annotation.RequireRole;
import com.neusoft.hospital.auth.enums.Role;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "药品信息管理", description = "药品字典的增删改查及拼音助记码快速检索接口")
@RestController
@RequestMapping("/api/v1/drug-info")
@RequiredArgsConstructor
@RequireRole({Role.ADMIN, Role.DOCTOR})
public class DrugInfoController {

    private final DrugInfoService drugInfoService;

    @Operation(summary = "分页查询药品列表", description = "支持按关键字和药品类型筛选")
    @GetMapping
    public Result<PageResult<DrugInfoResponse>> page(
            @Parameter(description = "页码，默认1", example = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数，默认10", example = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "关键字，模糊匹配药品名称或拼音助记码", example = "阿莫西林") @RequestParam(required = false) String keyword,
            @Parameter(description = "药品类型", example = "西药") @RequestParam(required = false) String drugType) {
        Page<DrugInfo> page = new Page<>(pageNum, pageSize);
        IPage<DrugInfo> result = drugInfoService.pageQuery(page, keyword, drugType);
        List<DrugInfoResponse> responses = result.getRecords().stream().map(this::toResponse).toList();
        return Result.ok(PageResult.of(result.getTotal(), pageNum, pageSize, responses));
    }

    @Operation(summary = "快速检索药品", description = "根据关键字快速检索药品名称或拼音助记码，返回列表(不分页)")
    @GetMapping("/search")
    public Result<List<DrugInfoResponse>> search(
            @Parameter(description = "关键字，模糊匹配药品名称或拼音助记码", example = "阿莫西林", required = true) @RequestParam String keyword) {
        List<DrugInfo> list = drugInfoService.searchByKeyword(keyword);
        List<DrugInfoResponse> responses = list.stream().map(this::toResponse).toList();
        return Result.ok(responses);
    }

    @Operation(summary = "根据ID查询药品详情", description = "通过药品ID获取完整信息")
    @GetMapping("/{id}")
    public Result<DrugInfoResponse> getById(
            @Parameter(description = "药品ID", example = "1", required = true) @PathVariable Integer id) {
        DrugInfo drugInfo = drugInfoService.getById(id);
        if (drugInfo == null) {
            return Result.fail(404, "药品不存在");
        }
        return Result.ok(toResponse(drugInfo));
    }

    @Operation(summary = "新增药品", description = "创建新的药品记录")
    @PostMapping
    public Result<Void> create(@RequestBody @Valid DrugInfoCreateRequest request) {
        DrugInfo drugInfo = new DrugInfo();
        BeanUtils.copyProperties(request, drugInfo);
        drugInfoService.save(drugInfo);
        return Result.ok();
    }

    @Operation(summary = "修改药品", description = "根据ID更新药品信息")
    @PutMapping
    public Result<Void> update(@RequestBody @Valid DrugInfoUpdateRequest request) {
        DrugInfo drugInfo = new DrugInfo();
        BeanUtils.copyProperties(request, drugInfo);
        drugInfoService.updateById(drugInfo);
        return Result.ok();
    }

    private DrugInfoResponse toResponse(DrugInfo entity) {
        DrugInfoResponse response = new DrugInfoResponse();
        BeanUtils.copyProperties(entity, response);
        return response;
    }
}
