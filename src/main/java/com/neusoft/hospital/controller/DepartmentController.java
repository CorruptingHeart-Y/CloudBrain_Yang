package com.neusoft.hospital.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.hospital.common.PageResult;
import com.neusoft.hospital.common.Result;
import com.neusoft.hospital.dto.request.DepartmentCreateRequest;
import com.neusoft.hospital.dto.request.DepartmentUpdateRequest;
import com.neusoft.hospital.dto.response.DepartmentResponse;
import com.neusoft.hospital.entity.Department;
import com.neusoft.hospital.service.DepartmentService;
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

@Tag(name = "科室管理", description = "科室字典的增删改查接口")
@RestController
@RequestMapping("/api/v1/department")
@RequiredArgsConstructor
@RequireRole(Role.ADMIN)
@SecurityRequirement(name = "Bearer")
public class DepartmentController {

    private final DepartmentService departmentService;

    @Operation(summary = "分页查询科室列表", description = "支持按科室名称模糊查询，默认按ID升序排列")
    @GetMapping
    public Result<PageResult<DepartmentResponse>> page(
            @Parameter(description = "页码，默认1", example = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数，默认10", example = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "科室名称，模糊匹配", example = "神经") @RequestParam(required = false) String deptName) {
        Page<Department> page = new Page<>(pageNum, pageSize);
        IPage<Department> result = departmentService.pageQuery(page, deptName);
        List<DepartmentResponse> responses = result.getRecords().stream().map(this::toResponse).toList();
        return Result.ok(PageResult.of(result.getTotal(), pageNum, pageSize, responses));
    }

    @Operation(summary = "查询全部科室(不分页)", description = "返回所有有效科室列表，用于下拉选择等场景")
    @GetMapping("/all")
    public Result<List<DepartmentResponse>> listAll() {
        List<Department> list = departmentService.listAll();
        List<DepartmentResponse> responses = list.stream().map(this::toResponse).toList();
        return Result.ok(responses);
    }

    @Operation(summary = "根据ID查询科室详情", description = "通过科室ID获取科室完整信息")
    @GetMapping("/{id}")
    public Result<DepartmentResponse> getById(
            @Parameter(description = "科室ID", example = "1", required = true) @PathVariable Integer id) {
        Department department = departmentService.getById(id);
        if (department == null) {
            return Result.fail(404, "科室不存在");
        }
        return Result.ok(toResponse(department));
    }

    @Operation(summary = "新增科室", description = "创建新的科室记录，科室编码必须唯一")
    @PostMapping
    public Result<Void> create(@RequestBody @Valid DepartmentCreateRequest request) {
        Department department = new Department();
        BeanUtils.copyProperties(request, department);
        departmentService.save(department);
        return Result.ok();
    }

    @Operation(summary = "修改科室", description = "根据ID更新科室信息")
    @PutMapping
    public Result<Void> update(@RequestBody @Valid DepartmentUpdateRequest request) {
        Department department = new Department();
        BeanUtils.copyProperties(request, department);
        departmentService.updateById(department);
        return Result.ok();
    }

    @Operation(summary = "删除科室(逻辑删除)", description = "将科室标记为已删除，delmark从1变为0")
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @Parameter(description = "科室ID", example = "1", required = true) @PathVariable Integer id) {
        departmentService.removeById(id);
        return Result.ok();
    }

    private DepartmentResponse toResponse(Department entity) {
        DepartmentResponse response = new DepartmentResponse();
        BeanUtils.copyProperties(entity, response);
        return response;
    }
}
