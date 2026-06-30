package com.neusoft.hospital.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.hospital.auth.annotation.RequireRole;
import com.neusoft.hospital.auth.enums.Role;
import com.neusoft.hospital.common.PageResult;
import com.neusoft.hospital.common.Result;
import com.neusoft.hospital.dto.request.RegisterCreateRequest;
import com.neusoft.hospital.dto.request.RegisterUpdateRequest;
import com.neusoft.hospital.dto.response.RegisterResponse;
import com.neusoft.hospital.entity.Department;
import com.neusoft.hospital.entity.Employee;
import com.neusoft.hospital.entity.Register;
import com.neusoft.hospital.entity.RegistLevel;
import com.neusoft.hospital.entity.SettleCategory;
import com.neusoft.hospital.auth.context.CurrentUser;
import com.neusoft.hospital.service.DepartmentService;
import com.neusoft.hospital.service.EmployeeService;
import com.neusoft.hospital.service.RegisterOwnership;
import com.neusoft.hospital.service.RegisterService;
import com.neusoft.hospital.service.RegistLevelService;
import com.neusoft.hospital.service.SettleCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "挂号管理", description = "患者挂号信息的增删改查及状态流转接口")
@RestController
@RequestMapping("/api/v1/register")
@RequiredArgsConstructor
@RequireRole({Role.ADMIN, Role.DOCTOR})
public class RegisterController {

    private final RegisterService registerService;
    private final DepartmentService departmentService;
    private final EmployeeService employeeService;
    private final RegistLevelService registLevelService;
    private final SettleCategoryService settleCategoryService;
    private final RegisterOwnership registerOwnership;

    @Operation(summary = "分页查询挂号列表", description = "支持按病历号精确查询、姓名模糊查询、看诊状态、就诊日期范围和科室筛选；DOCTOR 仅返回本人接诊记录")
    @GetMapping
    public Result<PageResult<RegisterResponse>> page(
            @Parameter(description = "页码，默认1", example = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数，默认10", example = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "病历号，精确匹配", example = "BL20240001") @RequestParam(required = false) String caseNumber,
            @Parameter(description = "患者姓名，模糊匹配", example = "张") @RequestParam(required = false) String realName,
            @Parameter(description = "看诊状态：1-已挂号 2-医生接诊 3-看诊结束 4-已退号", example = "1") @RequestParam(required = false) Integer visitState,
            @Parameter(description = "就诊日期起始（含），格式yyyy-MM-dd", example = "2024-01-01") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate visitDateStart,
            @Parameter(description = "就诊日期截止（含），格式yyyy-MM-dd", example = "2024-12-31") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate visitDateEnd,
            @Parameter(description = "科室ID", example = "1") @RequestParam(required = false) Integer deptmentId) {
        Page<Register> page = new Page<>(pageNum, pageSize);
        // PR4：DOCTOR 仅看本人接诊挂号；ADMIN 不限。employeeId 来自 CurrentUser，非前端入参
        IPage<Register> result = registerService.pageQuery(page, caseNumber, realName, visitState, visitDateStart, visitDateEnd, deptmentId, registerOwnership.doctorScopeEmployeeIdOrNull());
        List<RegisterResponse> responses = result.getRecords().stream().map(this::toResponse).toList();
        return Result.ok(PageResult.of(result.getTotal(), pageNum, pageSize, responses));
    }

    @Operation(summary = "根据病历号查询挂号信息", description = "通过病历号获取挂号完整信息，含科室名称、医生姓名、挂号级别名称和结算类别名称；DOCTOR 仅限本人接诊")
    @GetMapping("/case/{caseNumber}")
    public Result<RegisterResponse> getByCaseNumber(
            @Parameter(description = "病历号", example = "BL20240001", required = true) @PathVariable String caseNumber) {
        Register register = registerService.getByCaseNumber(caseNumber);
        if (register == null) {
            return Result.fail(404, "挂号记录不存在");
        }
        // 归属校验（DOCTOR 非本人 → 真实 404；ADMIN 任意），不命中抛 404
        registerOwnership.requireAccessibleRegister(register.getId());
        return Result.ok(toResponse(register));
    }

    @Operation(summary = "根据ID查询挂号详情", description = "通过挂号ID获取挂号完整信息；DOCTOR 仅限本人接诊，他人/不存在 → 404")
    @GetMapping("/{id}")
    public Result<RegisterResponse> getById(
            @Parameter(description = "挂号ID", example = "1", required = true) @PathVariable Integer id) {
        // 归属校验由 SQL WHERE id=? AND employee_id=? 完成（DOCTOR），不命中 → 真实 404
        Register register = registerOwnership.requireAccessibleRegister(id);
        return Result.ok(toResponse(register));
    }

    @Operation(summary = "新增挂号", description = "创建新的挂号记录；DOCTOR 仅能为本人接诊建号（强制 employeeId=当前医生），ADMIN 可指定任意医生")
    @PostMapping
    public Result<Void> create(@RequestBody @Valid RegisterCreateRequest request) {
        Register register = new Register();
        BeanUtils.copyProperties(request, register);
        // PR4：DOCTOR 不得通过 body 伪造 employeeId 为他人接诊；强制覆盖为当前医生
        if (CurrentUser.getAuthUser() != null && CurrentUser.getAuthUser().getRole() == Role.DOCTOR) {
            register.setEmployeeId(CurrentUser.getAuthUser().getEmployeeId());
        }
        registerService.save(register);
        return Result.ok();
    }

    @Operation(summary = "修改挂号信息", description = "根据ID更新挂号信息；DOCTOR 仅限本人接诊的挂号，他人/不存在 → 404")
    @PutMapping
    public Result<Void> update(@RequestBody RegisterUpdateRequest request) {
        // 先校验该挂号归属当前医生（DOCTOR），再更新
        registerOwnership.requireAccessibleRegister(request.getId());
        Register register = new Register();
        BeanUtils.copyProperties(request, register);
        registerService.updateById(register);
        return Result.ok();
    }

    @Operation(summary = "更新看诊状态", description = "根据挂号ID更新看诊状态；DOCTOR 仅限本人接诊的挂号，他人/不存在 → 404")
    @PutMapping("/{id}/state")
    public Result<Void> updateVisitState(
            @Parameter(description = "挂号ID", example = "1", required = true) @PathVariable Integer id,
            @Parameter(description = "看诊状态：1-已挂号 2-医生接诊 3-看诊结束 4-已退号", example = "2", required = true) @RequestParam Integer visitState) {
        registerOwnership.requireAccessibleRegister(id);
        Register register = new Register();
        register.setId(id);
        register.setVisitState(visitState);
        registerService.updateById(register);
        return Result.ok();
    }

    private RegisterResponse toResponse(Register entity) {
        RegisterResponse response = new RegisterResponse();
        BeanUtils.copyProperties(entity, response);
        if (entity.getDeptmentId() != null) {
            Department dept = departmentService.getById(entity.getDeptmentId());
            if (dept != null) {
                response.setDeptName(dept.getDeptName());
            }
        }
        if (entity.getEmployeeId() != null) {
            Employee employee = employeeService.getById(entity.getEmployeeId());
            if (employee != null) {
                response.setEmployeeName(employee.getRealname());
            }
        }
        if (entity.getRegistLevelId() != null) {
            RegistLevel level = registLevelService.getById(entity.getRegistLevelId());
            if (level != null) {
                response.setRegistLevelName(level.getRegistName());
            }
        }
        if (entity.getSettleCategoryId() != null) {
            SettleCategory category = settleCategoryService.getById(entity.getSettleCategoryId());
            if (category != null) {
                response.setSettleCategoryName(category.getSettleName());
            }
        }
        return response;
    }
}
