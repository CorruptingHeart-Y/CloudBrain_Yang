package com.neusoft.hospital.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.hospital.common.PageResult;
import com.neusoft.hospital.common.Result;
import com.neusoft.hospital.dto.request.MedicalTechnologyCreateRequest;
import com.neusoft.hospital.dto.request.MedicalTechnologyUpdateRequest;
import com.neusoft.hospital.dto.request.MedicalTechnologyQueryRequest;
import com.neusoft.hospital.dto.response.MedicalTechnologyResponse;
import com.neusoft.hospital.entity.Department;
import com.neusoft.hospital.entity.MedicalTechnology;
import com.neusoft.hospital.service.DepartmentService;
import com.neusoft.hospital.service.MedicalTechnologyService;
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

@Tag(name = "医技项目管理", description = "检查/检验/处置项目字典的增删改查接口")
@RestController
@RequestMapping("/api/v1/medical-technology")
@RequiredArgsConstructor
@RequireRole({Role.ADMIN, Role.DOCTOR})
public class MedicalTechnologyController {

    private final MedicalTechnologyService medicalTechnologyService;
    private final DepartmentService departmentService;

    @Operation(summary = "分页查询医技项目列表", description = "支持按项目名称、项目类型、所属科室筛选")
    @GetMapping
    public Result<PageResult<MedicalTechnologyResponse>> page(
            @Parameter(description = "页码，默认1", example = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数，默认10", example = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "项目名称，模糊匹配", example = "血常规") @RequestParam(required = false) String techName,
            @Parameter(description = "项目类型", example = "检查") @RequestParam(required = false) String techType,
            @Parameter(description = "科室ID", example = "1") @RequestParam(required = false) Integer deptmentId) {
        Page<MedicalTechnology> page = new Page<>(pageNum, pageSize);
        IPage<MedicalTechnology> result = medicalTechnologyService.pageQuery(page, techName, techType, deptmentId);
        List<MedicalTechnologyResponse> responses = result.getRecords().stream().map(this::toResponse).toList();
        return Result.ok(PageResult.of(result.getTotal(), pageNum, pageSize, responses));
    }

    @Operation(summary = "查询全部医技项目(不分页)", description = "返回所有医技项目列表，用于下拉选择等场景")
    @GetMapping("/all")
    public Result<List<MedicalTechnologyResponse>> listAll() {
        List<MedicalTechnology> list = medicalTechnologyService.listAll();
        List<MedicalTechnologyResponse> responses = list.stream().map(this::toResponse).toList();
        return Result.ok(responses);
    }

    @Operation(summary = "根据ID查询医技项目详情", description = "通过医技项目ID获取完整信息")
    @GetMapping("/{id}")
    public Result<MedicalTechnologyResponse> getById(
            @Parameter(description = "医技项目ID", example = "1", required = true) @PathVariable Integer id) {
        MedicalTechnology medicalTechnology = medicalTechnologyService.getById(id);
        if (medicalTechnology == null) {
            return Result.fail(404, "医技项目不存在");
        }
        return Result.ok(toResponse(medicalTechnology));
    }

    @Operation(summary = "新增医技项目", description = "创建新的医技项目记录")
    @PostMapping
    public Result<Void> create(@RequestBody @Valid MedicalTechnologyCreateRequest request) {
        MedicalTechnology medicalTechnology = new MedicalTechnology();
        BeanUtils.copyProperties(request, medicalTechnology);
        medicalTechnologyService.save(medicalTechnology);
        return Result.ok();
    }

    @Operation(summary = "修改医技项目", description = "根据ID更新医技项目信息")
    @PutMapping
    public Result<Void> update(@RequestBody @Valid MedicalTechnologyUpdateRequest request) {
        MedicalTechnology medicalTechnology = new MedicalTechnology();
        BeanUtils.copyProperties(request, medicalTechnology);
        medicalTechnologyService.updateById(medicalTechnology);
        return Result.ok();
    }

    private MedicalTechnologyResponse toResponse(MedicalTechnology entity) {
        MedicalTechnologyResponse response = new MedicalTechnologyResponse();
        BeanUtils.copyProperties(entity, response);
        if (entity.getDeptmentId() != null) {
            Department department = departmentService.getById(entity.getDeptmentId());
            if (department != null) {
                response.setDeptName(department.getDeptName());
            }
        }
        return response;
    }
}
