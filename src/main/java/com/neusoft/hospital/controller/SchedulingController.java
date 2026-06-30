package com.neusoft.hospital.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.hospital.common.PageResult;
import com.neusoft.hospital.common.Result;
import com.neusoft.hospital.dto.request.SchedulingCreateRequest;
import com.neusoft.hospital.dto.request.SchedulingUpdateRequest;
import com.neusoft.hospital.dto.response.SchedulingResponse;
import com.neusoft.hospital.entity.Scheduling;
import com.neusoft.hospital.service.SchedulingService;
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

@Tag(name = "排班管理", description = "排班规则的增删改查接口")
@RestController
@RequestMapping("/api/v1/scheduling")
@RequiredArgsConstructor
@RequireRole(Role.ADMIN)
public class SchedulingController {

    private final SchedulingService schedulingService;

    @Operation(summary = "分页查询排班列表", description = "按ID升序排列")
    @GetMapping
    public Result<PageResult<SchedulingResponse>> page(
            @Parameter(description = "页码，默认1", example = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数，默认10", example = "10") @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<Scheduling> page = new Page<>(pageNum, pageSize);
        IPage<Scheduling> result = schedulingService.pageQuery(page);
        List<SchedulingResponse> responses = result.getRecords().stream().map(this::toResponse).toList();
        return Result.ok(PageResult.of(result.getTotal(), pageNum, pageSize, responses));
    }

    @Operation(summary = "查询全部排班(不分页)", description = "返回所有有效排班列表，用于下拉选择等场景")
    @GetMapping("/all")
    public Result<List<SchedulingResponse>> listAll() {
        List<Scheduling> list = schedulingService.listAll();
        List<SchedulingResponse> responses = list.stream().map(this::toResponse).toList();
        return Result.ok(responses);
    }

    @Operation(summary = "根据ID查询排班详情", description = "通过排班ID获取完整信息")
    @GetMapping("/{id}")
    public Result<SchedulingResponse> getById(
            @Parameter(description = "排班ID", example = "1", required = true) @PathVariable Integer id) {
        Scheduling scheduling = schedulingService.getById(id);
        if (scheduling == null) {
            return Result.fail(404, "排班不存在");
        }
        return Result.ok(toResponse(scheduling));
    }

    @Operation(summary = "新增排班", description = "创建新的排班记录")
    @PostMapping
    public Result<Void> create(@RequestBody @Valid SchedulingCreateRequest request) {
        Scheduling scheduling = new Scheduling();
        BeanUtils.copyProperties(request, scheduling);
        schedulingService.save(scheduling);
        return Result.ok();
    }

    @Operation(summary = "修改排班", description = "根据ID更新排班信息")
    @PutMapping
    public Result<Void> update(@RequestBody @Valid SchedulingUpdateRequest request) {
        Scheduling scheduling = new Scheduling();
        BeanUtils.copyProperties(request, scheduling);
        schedulingService.updateById(scheduling);
        return Result.ok();
    }

    @Operation(summary = "删除排班(逻辑删除)", description = "将排班标记为已删除，delmark从1变为0")
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @Parameter(description = "排班ID", example = "1", required = true) @PathVariable Integer id) {
        schedulingService.removeById(id);
        return Result.ok();
    }

    private SchedulingResponse toResponse(Scheduling entity) {
        SchedulingResponse response = new SchedulingResponse();
        BeanUtils.copyProperties(entity, response);
        return response;
    }
}
