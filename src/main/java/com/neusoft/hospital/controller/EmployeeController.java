package com.neusoft.hospital.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.hospital.common.PageResult;
import com.neusoft.hospital.common.Result;
import com.neusoft.hospital.dto.request.EmployeeCreateRequest;
import com.neusoft.hospital.dto.request.EmployeeUpdateRequest;
import com.neusoft.hospital.dto.response.EmployeeResponse;
import com.neusoft.hospital.entity.Department;
import com.neusoft.hospital.entity.Employee;
import com.neusoft.hospital.entity.RegistLevel;
import com.neusoft.hospital.service.DepartmentService;
import com.neusoft.hospital.service.EmployeeService;
import com.neusoft.hospital.service.RegistLevelService;
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

@Tag(name = "员工管理", description = "医院员工(医生)的增删改查接口")
@RestController
@RequestMapping("/api/v1/employee")
@RequiredArgsConstructor
@RequireRole(Role.ADMIN)
public class EmployeeController {

    private final EmployeeService employeeService;
    private final DepartmentService departmentService;
    private final RegistLevelService registLevelService;

    @Operation(summary = "分页查询员工列表", description = "支持按姓名模糊查询和科室筛选，默认按ID升序")
    @GetMapping
    public Result<PageResult<EmployeeResponse>> page(
            @Parameter(description = "页码，默认1", example = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数，默认10", example = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "员工姓名，模糊匹配", example = "李") @RequestParam(required = false) String realname,
            @Parameter(description = "所在科室ID", example = "1") @RequestParam(required = false) Integer deptmentId) {
        Page<Employee> page = new Page<>(pageNum, pageSize);
        IPage<Employee> result = employeeService.pageQuery(page, realname, deptmentId);
        List<EmployeeResponse> responses = result.getRecords().stream().map(this::toResponse).toList();
        return Result.ok(PageResult.of(result.getTotal(), pageNum, pageSize, responses));
    }

    @Operation(summary = "按科室查询员工列表", description = "返回指定科室下的所有有效员工，用于选择医生下拉框等场景")
    @GetMapping("/dept/{deptId}")
    public Result<List<EmployeeResponse>> listByDept(
            @Parameter(description = "科室ID", example = "1", required = true) @PathVariable Integer deptId) {
        List<Employee> list = employeeService.listByDeptId(deptId);
        List<EmployeeResponse> responses = list.stream().map(this::toResponse).toList();
        return Result.ok(responses);
    }

    @Operation(summary = "根据ID查询员工详情", description = "通过员工ID获取员工完整信息，含科室名称和挂号级别名称")
    @GetMapping("/{id}")
    public Result<EmployeeResponse> getById(
            @Parameter(description = "员工ID", example = "1", required = true) @PathVariable Integer id) {
        Employee employee = employeeService.getById(id);
        if (employee == null) {
            return Result.fail(404, "员工不存在");
        }
        return Result.ok(toResponse(employee));
    }

    @Operation(summary = "新增员工", description = "创建新的员工记录，姓名和密码为必填")
    @PostMapping
    public Result<Void> create(@RequestBody @Valid EmployeeCreateRequest request) {
        Employee employee = new Employee();
        BeanUtils.copyProperties(request, employee);
        employeeService.save(employee);
        return Result.ok();
    }

    @Operation(summary = "修改员工信息", description = "根据ID更新员工信息")
    @PutMapping
    public Result<Void> update(@RequestBody @Valid EmployeeUpdateRequest request) {
        Employee employee = new Employee();
        BeanUtils.copyProperties(request, employee);
        employeeService.updateById(employee);
        return Result.ok();
    }

    @Operation(summary = "删除员工(逻辑删除)", description = "将员工标记为已删除，delmark从1变为0")
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @Parameter(description = "员工ID", example = "1", required = true) @PathVariable Integer id) {
        employeeService.removeById(id);
        return Result.ok();
    }

    private EmployeeResponse toResponse(Employee entity) {
        EmployeeResponse response = new EmployeeResponse();
        BeanUtils.copyProperties(entity, response);
        if (entity.getDeptmentId() != null) {
            Department dept = departmentService.getById(entity.getDeptmentId());
            if (dept != null) {
                response.setDeptName(dept.getDeptName());
            }
        }
        if (entity.getRegistLevelId() != null) {
            RegistLevel level = registLevelService.getById(entity.getRegistLevelId());
            if (level != null) {
                response.setRegistLevelName(level.getRegistName());
            }
        }
        return response;
    }
}
