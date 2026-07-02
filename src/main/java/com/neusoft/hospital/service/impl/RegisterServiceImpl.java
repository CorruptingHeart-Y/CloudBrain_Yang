package com.neusoft.hospital.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neusoft.hospital.common.BusinessException;
import com.neusoft.hospital.common.ErrorCode;
import com.neusoft.hospital.dto.request.RegisterCreateRequest;
import com.neusoft.hospital.entity.Register;
import com.neusoft.hospital.entity.RegistLevel;
import com.neusoft.hospital.mapper.RegisterMapper;
import com.neusoft.hospital.service.PatientRegisterLinkService;
import com.neusoft.hospital.service.RegisterService;
import com.neusoft.hospital.service.RegistLevelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterServiceImpl extends ServiceImpl<RegisterMapper, Register> implements RegisterService {

    /** case_number 生成冲突时的最大重试次数。 */
    private static final int CASE_NUMBER_MAX_RETRY = 3;
    private static final DateTimeFormatter CASE_TS_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final RegistLevelService registLevelService;
    private final PatientRegisterLinkService patientRegisterLinkService;
    private final com.neusoft.hospital.service.QuotaService quotaService;

    @Override
    public IPage<Register> pageQuery(Page<Register> page, String caseNumber, String realName, Integer visitState, LocalDate visitDateStart, LocalDate visitDateEnd, Integer deptmentId, Integer scopeEmployeeId, List<Integer> scopeRegisterIds) {
        LambdaQueryWrapper<Register> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(caseNumber)) {
            wrapper.like(Register::getCaseNumber, caseNumber);
        }
        if (StringUtils.hasText(realName)) {
            wrapper.like(Register::getRealName, realName);
        }
        if (visitState != null) {
            wrapper.eq(Register::getVisitState, visitState);
        }
        if (visitDateStart != null) {
            wrapper.ge(Register::getVisitDate, visitDateStart);
        }
        if (visitDateEnd != null) {
            wrapper.le(Register::getVisitDate, visitDateEnd);
        }
        if (deptmentId != null) {
            wrapper.eq(Register::getDeptmentId, deptmentId);
        }
        // DOCTOR 范围：scopeEmployeeId 由 CurrentUser 注入，非前端入参
        if (scopeEmployeeId != null) {
            wrapper.eq(Register::getEmployeeId, scopeEmployeeId);
        }
        // PATIENT 范围：仅返回桥接表内 link 到本人的 register；空 list 表示无 link → 返回空页
        if (scopeRegisterIds != null) {
            if (scopeRegisterIds.isEmpty()) {
                wrapper.eq(Register::getId, -1); // 永假条件，保证空结果
            } else {
                wrapper.in(Register::getId, scopeRegisterIds);
            }
        }
        wrapper.orderByDesc(Register::getId);
        return this.page(page, wrapper);
    }

    @Override
    public Register getByCaseNumber(String caseNumber) {
        LambdaQueryWrapper<Register> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Register::getCaseNumber, caseNumber);
        return this.getOne(wrapper);
    }

    @Override
    public boolean updateVisitState(Integer id, Integer visitState) {
        Register register = this.getById(id);
        if (register == null) {
            return false;
        }
        register.setVisitState(visitState);
        return this.updateById(register);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Register createRegister(RegisterCreateRequest request, Integer employeeId) {
        // 1. 校验挂号级别存在，并据此推导挂号费（顺带做外键存在性校验）
        RegistLevel level = registLevelService.getById(request.getRegistLevelId());
        if (level == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "挂号级别不存在: " + request.getRegistLevelId());
        }

        // 2. 组装挂号实体（DTO 已无 caseNumber/registMoney，不会被覆盖）
        Register register = new Register();
        org.springframework.beans.BeanUtils.copyProperties(request, register);
        register.setEmployeeId(employeeId);
        register.setRegistMoney(level.getRegistFee());
        register.setVisitState(1); // 建号即「已挂号」

        // 3. 生成唯一 case_number 并落库（冲突重试）
        saveWithGeneratedCaseNumber(register);

        // 4. 身份匹配则自动建 link（事务内；link 非幂等异常会回滚挂号）
        patientRegisterLinkService.linkIfMatched(register);

        return register;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Register createRegisterWithQuota(RegisterCreateRequest request, Integer employeeId) {
        // 号源扣减：已放号则扣（满号抛 409），未放号放行（向后兼容）
        java.time.LocalDate quotaDate = request.getVisitDate() != null
                ? request.getVisitDate().toLocalDate() : java.time.LocalDate.now();
        String noon = request.getNoon() != null ? request.getNoon() : "上午";
        if (employeeId != null) {
            boolean deducted = quotaService.deductDbOrThrow(employeeId, quotaDate, noon);
            if (deducted) {
                // 同步 Redis 库存（若 key 存在则 DECR，防抢号窗口漂移）
                quotaService.decrRedisIfPresent(employeeId, quotaDate, noon);
            }
        }
        return createRegister(request, employeeId);
    }

    /**
     * 生成 case_number（BL+yyyyMMddHHmmss+2位随机）并保存；
     * 命中 uk_case_number 唯一索引冲突时重新生成重试，最多 {@value #CASE_NUMBER_MAX_RETRY} 次。
     */
    private void saveWithGeneratedCaseNumber(Register register) {
        DuplicateKeyException last = null;
        for (int attempt = 0; attempt < CASE_NUMBER_MAX_RETRY; attempt++) {
            register.setCaseNumber(generateCaseNumber());
            try {
                this.save(register);
                return;
            } catch (DuplicateKeyException e) {
                last = e;
                log.warn("case_number 生成冲突（attempt={}, value={}），重试", attempt + 1, register.getCaseNumber());
            }
        }
        throw new BusinessException(ErrorCode.CONFLICT.getCode(), "病历号生成冲突，请重试");
    }

    private String generateCaseNumber() {
        String ts = LocalDateTime.now().format(CASE_TS_FMT);
        int rand = ThreadLocalRandom.current().nextInt(0, 100); // 00-99
        return "BL" + ts + String.format("%02d", rand);
    }
}
