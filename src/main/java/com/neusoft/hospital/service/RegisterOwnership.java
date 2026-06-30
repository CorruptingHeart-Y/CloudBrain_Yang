package com.neusoft.hospital.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neusoft.hospital.auth.context.AuthUser;
import com.neusoft.hospital.auth.context.CurrentUser;
import com.neusoft.hospital.auth.enums.Role;
import com.neusoft.hospital.common.BusinessException;
import com.neusoft.hospital.common.ErrorCode;
import com.neusoft.hospital.entity.Register;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 挂号医生归属校验组件（PR4）。
 * <p>
 * 用于 ADMIN + DOCTOR 共享的诊疗业务接口，落实“医生只能访问本人接诊挂号(register.employee_id = 当前医生)”
 * 的数据范围隔离。归属判定由 SQL 条件完成，不在 Java 内存中比较 employeeId。
 * <ul>
 *   <li>DOCTOR：以 {@code WHERE id=? AND employee_id=currentUser.employeeId} 单条 SQL 校验，不命中→404；</li>
 *   <li>ADMIN：不加 employee_id 过滤，跨医生可访问（仅校验记录存在）；</li>
 *   <li>PATIENT：不会到达本组件（Controller 类级 @RequireRole 已在拦截器层 403）。</li>
 * </ul>
 * 不存在 / 不属于当前医生的 registerId 一律返回 404，不透露记录是否存在。
 * 不信任前端传入的 employeeId；employeeId 只取自已验证 JWT 的 CurrentUser。
 */
@Component
@RequiredArgsConstructor
public class RegisterOwnership {

    private final RegisterService registerService;

    /**
     * 校验 registerId 对当前用户可访问并返回该挂号。
     * DOCTOR 必须本人接诊；ADMIN 任意。不可访问/不存在 → 抛 404。
     */
    public Register requireAccessibleRegister(Integer registerId) {
        if (registerId == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        AuthUser user = CurrentUser.requireAuthUser();
        LambdaQueryWrapper<Register> wrapper = new LambdaQueryWrapper<Register>()
                .eq(Register::getId, registerId);
        if (user.getRole() == Role.DOCTOR) {
            Integer employeeId = user.getEmployeeId();
            if (employeeId == null) {
                // DOCTOR 账号未绑定 employee，按不可访问处理
                throw new BusinessException(ErrorCode.NOT_FOUND);
            }
            // 归属判定下沉到 SQL：employee_id 条件不命中即返回 null → 404
            wrapper.eq(Register::getEmployeeId, employeeId);
        }
        // ADMIN：不加 employee_id 过滤，跨医生可访问
        Register register = registerService.getOne(wrapper);
        if (register == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return register;
    }

    /**
     * 当前若为 DOCTOR，返回其 employeeId 用于分页列表范围过滤；否则（ADMIN/其他）返回 null 表示不限范围。
     */
    public Integer doctorScopeEmployeeIdOrNull() {
        AuthUser user = CurrentUser.getAuthUser();
        if (user == null || user.getRole() != Role.DOCTOR) {
            return null;
        }
        return user.getEmployeeId();
    }
}
