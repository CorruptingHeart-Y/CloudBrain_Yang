package com.neusoft.hospital.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neusoft.hospital.dto.request.RegisterCreateRequest;
import com.neusoft.hospital.entity.Register;

import java.time.LocalDate;
import java.util.List;

public interface RegisterService extends IService<Register> {

    IPage<Register> pageQuery(Page<Register> page, String caseNumber, String realName, Integer visitState, LocalDate visitDateStart, LocalDate visitDateEnd, Integer deptmentId, Integer scopeEmployeeId, List<Integer> scopeRegisterIds);

    Register getByCaseNumber(String caseNumber);

    boolean updateVisitState(Integer id, Integer visitState);

    /**
     * 创建挂号（事务）。
     * <p>服务端负责：生成唯一 case_number（BL+yyyyMMddHHmmss+2位随机，冲突重试）、
     * 按 registLevelId 推导 regist_money、置 visit_state=1、身份匹配则建 patient_register_link。
     * @param request   挂号创建请求（不含 caseNumber/registMoney）
     * @param employeeId 已解析的接诊医生ID（DOCTOR 由 Controller 强制为当前医生，ADMIN 取 request.employeeId）
     * @return 回填 id/caseNumber 的挂号实体
     */
    Register createRegister(RegisterCreateRequest request, Integer employeeId);

    /**
     * 现场挂号（ADMIN/DOCTOR）：先扣号源（已放号则扣，未放号放行），再 createRegister。
     * <p>扣减与落库同事务，原子。满号 → BusinessException(409)。
     */
    Register createRegisterWithQuota(RegisterCreateRequest request, Integer employeeId);
}
