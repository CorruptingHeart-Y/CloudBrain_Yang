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

import java.util.List;

/**
 * 挂号归属校验组件（v2.0，三角色）。
 * <p>
 * 用于诊疗业务接口，落实数据范围隔离，归属判定下沉到 SQL，不在 Java 内存比较身份字段。
 * <ul>
 *   <li>DOCTOR：{@code WHERE id=? AND employee_id=currentUser.employeeId}，不命中→404；</li>
 *   <li>PATIENT：经 patient_register_link 校验 (patientId, registerId) 是否存在，不命中→404；</li>
 *   <li>ADMIN：不加过滤，跨医生/跨患者可访问（仅校验记录存在）。</li>
 * </ul>
 * 不存在 / 不属于当前用户的 registerId 一律 404，不透露记录是否存在。
 * 不信任前端传入的 employeeId / patientId；身份只取自已验证 JWT 的 CurrentUser。
 */
@Component
@RequiredArgsConstructor
public class RegisterOwnership {

    private final RegisterService registerService;
    private final PatientRegisterLinkService patientRegisterLinkService;

    /**
     * 校验 registerId 对当前用户可访问并返回该挂号。
     * DOCTOR 限本人接诊；PATIENT 限本人 link；ADMIN 任意。不可访问/不存在 → 404。
     */
    public Register requireAccessibleRegister(Integer registerId) {
        if (registerId == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        AuthUser user = CurrentUser.requireAuthUser();

        // PATIENT：经桥接表校验归属，不读 register 本身的字段
        if (user.getRole() == Role.PATIENT) {
            if (user.getPatientId() == null
                    || !patientRegisterLinkService.existsLink(user.getPatientId(), registerId)) {
                throw new BusinessException(ErrorCode.NOT_FOUND);
            }
            Register register = registerService.getById(registerId);
            if (register == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND);
            }
            return register;
        }

        // DOCTOR / ADMIN
        LambdaQueryWrapper<Register> wrapper = new LambdaQueryWrapper<Register>()
                .eq(Register::getId, registerId);
        if (user.getRole() == Role.DOCTOR) {
            Integer employeeId = user.getEmployeeId();
            if (employeeId == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND);
            }
            wrapper.eq(Register::getEmployeeId, employeeId);
        }
        Register register = registerService.getOne(wrapper);
        if (register == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return register;
    }

    /** DOCTOR：返回 employeeId 用于分页范围过滤；其他角色返回 null。 */
    public Integer doctorScopeEmployeeIdOrNull() {
        AuthUser user = CurrentUser.getAuthUser();
        if (user == null || user.getRole() != Role.DOCTOR) {
            return null;
        }
        return user.getEmployeeId();
    }

    /**
     * PATIENT：返回其 link 内的 register_id 列表用于分页范围过滤；
     * 空 list 表示该患者无任何 link（分页应返回空）；非 PATIENT 返回 null（不过滤）。
     */
    public List<Integer> patientScopeRegisterIdsOrNull() {
        AuthUser user = CurrentUser.getAuthUser();
        if (user == null || user.getRole() != Role.PATIENT) {
            return null;
        }
        return patientRegisterLinkService.findRegisterIdsByPatient(user.getPatientId());
    }
}
