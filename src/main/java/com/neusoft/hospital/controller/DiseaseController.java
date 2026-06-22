package com.neusoft.hospital.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.hospital.common.PageResult;
import com.neusoft.hospital.common.Result;
import com.neusoft.hospital.dto.request.DiseaseCreateRequest;
import com.neusoft.hospital.dto.request.DiseaseUpdateRequest;
import com.neusoft.hospital.dto.response.DiseaseResponse;
import com.neusoft.hospital.entity.Disease;
import com.neusoft.hospital.service.DiseaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "疾病管理", description = "疾病字典(含ICD编码)的增删改查及快速检索接口")
@RestController
@RequestMapping("/api/v1/disease")
@RequiredArgsConstructor
public class DiseaseController {

    private final DiseaseService diseaseService;

    @Operation(summary = "分页查询疾病列表", description = "支持按疾病名称或ICD编码模糊查询")
    @GetMapping
    public Result<PageResult<DiseaseResponse>> page(
            @Parameter(description = "页码，默认1", example = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数，默认10", example = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "关键字，模糊匹配疾病名称或ICD编码", example = "肺炎") @RequestParam(required = false) String keyword) {
        Page<Disease> page = new Page<>(pageNum, pageSize);
        IPage<Disease> result = diseaseService.pageQuery(page, keyword);
        List<DiseaseResponse> responses = result.getRecords().stream().map(this::toResponse).toList();
        return Result.ok(PageResult.of(result.getTotal(), pageNum, pageSize, responses));
    }

    @Operation(summary = "快速检索疾病", description = "根据关键字快速检索疾病名称或ICD编码，返回列表(不分页)")
    @GetMapping("/search")
    public Result<List<DiseaseResponse>> search(
            @Parameter(description = "关键字，模糊匹配疾病名称或ICD编码", example = "肺炎", required = true) @RequestParam String keyword) {
        List<Disease> list = diseaseService.searchByKeyword(keyword);
        List<DiseaseResponse> responses = list.stream().map(this::toResponse).toList();
        return Result.ok(responses);
    }

    @Operation(summary = "根据ID查询疾病详情", description = "通过疾病ID获取完整信息")
    @GetMapping("/{id}")
    public Result<DiseaseResponse> getById(
            @Parameter(description = "疾病ID", example = "1", required = true) @PathVariable Integer id) {
        Disease disease = diseaseService.getById(id);
        if (disease == null) {
            return Result.fail(404, "疾病不存在");
        }
        return Result.ok(toResponse(disease));
    }

    @Operation(summary = "新增疾病", description = "创建新的疾病记录")
    @PostMapping
    public Result<Void> create(@RequestBody @Valid DiseaseCreateRequest request) {
        Disease disease = new Disease();
        BeanUtils.copyProperties(request, disease);
        diseaseService.save(disease);
        return Result.ok();
    }

    @Operation(summary = "修改疾病", description = "根据ID更新疾病信息")
    @PutMapping
    public Result<Void> update(@RequestBody @Valid DiseaseUpdateRequest request) {
        Disease disease = new Disease();
        BeanUtils.copyProperties(request, disease);
        diseaseService.updateById(disease);
        return Result.ok();
    }

    private DiseaseResponse toResponse(Disease entity) {
        DiseaseResponse response = new DiseaseResponse();
        BeanUtils.copyProperties(entity, response);
        return response;
    }
}
