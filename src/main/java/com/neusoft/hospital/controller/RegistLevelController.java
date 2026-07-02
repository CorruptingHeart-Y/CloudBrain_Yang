package com.neusoft.hospital.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.hospital.common.PageResult;
import com.neusoft.hospital.common.Result;
import com.neusoft.hospital.dto.request.RegistLevelCreateRequest;
import com.neusoft.hospital.dto.request.RegistLevelUpdateRequest;
import com.neusoft.hospital.dto.response.RegistLevelResponse;
import com.neusoft.hospital.entity.RegistLevel;
import com.neusoft.hospital.service.RegistLevelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.neusoft.hospital.auth.annotation.RequireRole;
import com.neusoft.hospital.auth.enums.Role;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "挂号级别管理", description = "挂号级别字典的增删改查接口")
@RestController
@RequestMapping("/api/v1/regist-level")
@RequiredArgsConstructor
@RequireRole(Role.ADMIN)
@SecurityRequirement(name = "Bearer")
public class RegistLevelController {

    private final RegistLevelService registLevelService;

    @Operation(summary = "分页查询挂号级别列表", description = "按ID升序排列")
    @GetMapping
    public Result<PageResult<RegistLevelResponse>> page(
            @Parameter(description = "页码，默认1", example = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数，默认10", example = "10") @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<RegistLevel> page = new Page<>(pageNum, pageSize);
        IPage<RegistLevel> result = registLevelService.pageQuery(page);
        List<RegistLevelResponse> responses = result.getRecords().stream().map(this::toResponse).toList();
        return Result.ok(PageResult.of(result.getTotal(), pageNum, pageSize, responses));
    }

    @Operation(summary = "查询全部挂号级别(不分页)", description = "返回所有有效挂号级别列表，用于下拉选择等场景；三角色均可用（挂号表单/抢号页下拉）")
    @GetMapping("/all")
    @RequireRole({Role.ADMIN, Role.DOCTOR, Role.PATIENT})
    public Result<List<RegistLevelResponse>> listAll() {
        List<RegistLevel> list = registLevelService.listAll();
        List<RegistLevelResponse> responses = list.stream().map(this::toResponse).toList();
        return Result.ok(responses);
    }

    @Operation(summary = "根据ID查询挂号级别详情", description = "通过挂号级别ID获取完整信息")
    @GetMapping("/{id}")
    public Result<RegistLevelResponse> getById(
            @Parameter(description = "挂号级别ID", example = "1", required = true) @PathVariable Integer id) {
        RegistLevel registLevel = registLevelService.getById(id);
        if (registLevel == null) {
            return Result.fail(404, "挂号级别不存在");
        }
        return Result.ok(toResponse(registLevel));
    }

    @Operation(summary = "新增挂号级别", description = "创建新的挂号级别记录")
    @PostMapping
    public Result<Void> create(@RequestBody @Valid RegistLevelCreateRequest request) {
        RegistLevel registLevel = new RegistLevel();
        BeanUtils.copyProperties(request, registLevel);
        registLevelService.save(registLevel);
        return Result.ok();
    }

    @Operation(summary = "修改挂号级别", description = "根据ID更新挂号级别信息")
    @PutMapping
    public Result<Void> update(@RequestBody @Valid RegistLevelUpdateRequest request) {
        RegistLevel registLevel = new RegistLevel();
        BeanUtils.copyProperties(request, registLevel);
        registLevelService.updateById(registLevel);
        return Result.ok();
    }

    @Operation(summary = "删除挂号级别(逻辑删除)", description = "将挂号级别标记为已删除，delmark从1变为0")
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @Parameter(description = "挂号级别ID", example = "1", required = true) @PathVariable Integer id) {
        registLevelService.removeById(id);
        return Result.ok();
    }

    private RegistLevelResponse toResponse(RegistLevel entity) {
        RegistLevelResponse response = new RegistLevelResponse();
        BeanUtils.copyProperties(entity, response);
        return response;
    }
}
