package com.neusoft.hospital.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neusoft.hospital.entity.Patient;
import com.neusoft.hospital.entity.PatientRegisterLink;
import com.neusoft.hospital.entity.Register;
import com.neusoft.hospital.mapper.PatientMapper;
import com.neusoft.hospital.mapper.PatientRegisterLinkMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * 患者↔挂号 桥接表服务（v2.0）。
 * <p>
 * 患者归属经 patient_register_link 表达，不依赖 register.patient_id。
 * <ul>
 *   <li>查询某患者的全部 register_id；</li>
 *   <li>校验某 (patientId, registerId) 是否存在 link；</li>
 *   <li>新建挂号后，如登记身份与某 patient 严格匹配则自动建立 link（不创建 patient）。</li>
 * </ul>
 * 不信任前端传入的 patientId；patientId 只取自已验证 JWT。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PatientRegisterLinkService {

    private final PatientRegisterLinkMapper linkMapper;
    private final PatientMapper patientMapper;

    /** 某患者的全部 register_id（按 id 升序）。 */
    public List<Integer> findRegisterIdsByPatient(Integer patientId) {
        if (patientId == null) {
            return List.of();
        }
        return linkMapper.selectList(new LambdaQueryWrapper<PatientRegisterLink>()
                        .eq(PatientRegisterLink::getPatientId, patientId)
                        .orderByAsc(PatientRegisterLink::getRegisterId))
                .stream().map(PatientRegisterLink::getRegisterId).toList();
    }

    /** 是否存在 (patientId, registerId) 的 link。 */
    public boolean existsLink(Integer patientId, Integer registerId) {
        if (patientId == null || registerId == null) {
            return false;
        }
        Long c = linkMapper.selectCount(new LambdaQueryWrapper<PatientRegisterLink>()
                .eq(PatientRegisterLink::getPatientId, patientId)
                .eq(PatientRegisterLink::getRegisterId, registerId));
        return c != null && c > 0;
    }

    /**
     * 新建挂号后自动 link：仅当存在与挂号身份(card_number+real_name+gender+birthdate)
     * 严格一致的 patient 时，建立 link。不创建 patient；不匹配则不 link（待患者注册或人工处理）。
     * 幂等：若该 register 已有 link，uk_register_id 冲突时忽略。
     */
    public void linkIfMatched(Register register) {
        if (register == null || register.getId() == null
                || register.getCardNumber() == null || register.getCardNumber().isBlank()
                || register.getRealName() == null || register.getRealName().isBlank()
                || register.getGender() == null || register.getBirthdate() == null) {
            return; // 身份不完整，不自动 link
        }
        Patient patient = patientMapper.selectOne(new LambdaQueryWrapper<Patient>()
                .eq(Patient::getCardNumber, register.getCardNumber()));
        if (patient == null) {
            return; // 无对应 patient，待患者自助注册后再 link
        }
        if (!Objects.equals(patient.getRealName(), register.getRealName())
                || !Objects.equals(patient.getGender(), register.getGender())
                || !Objects.equals(patient.getBirthdate(), register.getBirthdate())) {
            log.warn("register.id={} 与 patient.id={} 身份资料不一致，不自动 link，待人工处理",
                    register.getId(), patient.getId());
            return; // 身份冲突，不 link
        }
        PatientRegisterLink link = new PatientRegisterLink();
        link.setPatientId(patient.getId());
        link.setRegisterId(register.getId());
        link.setLinkSource("AUTO_CREATE");
        link.setCreateTime(java.time.LocalDateTime.now());
        try {
            linkMapper.insert(link);
        } catch (DuplicateKeyException e) {
            // 该 register 已有 link，幂等忽略（不覆盖既有 link）
        }
    }
}
