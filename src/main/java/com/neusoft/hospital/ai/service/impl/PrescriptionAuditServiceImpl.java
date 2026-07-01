package com.neusoft.hospital.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neusoft.hospital.ai.client.PrescriptionAuditClient;
import com.neusoft.hospital.ai.dto.AiPrescriptionCheckRequest;
import com.neusoft.hospital.ai.dto.AiPrescriptionCheckResponse;
import com.neusoft.hospital.ai.dto.PatientBrief;
import com.neusoft.hospital.ai.dto.PrescriptionAuditRecordDTO;
import com.neusoft.hospital.ai.dto.PrescriptionAuditResultDTO;
import com.neusoft.hospital.ai.service.PrescriptionAuditService;
import com.neusoft.hospital.auth.context.CurrentUser;
import com.neusoft.hospital.common.BusinessException;
import com.neusoft.hospital.common.ErrorCode;
import com.neusoft.hospital.entity.DrugInfo;
import com.neusoft.hospital.entity.Prescription;
import com.neusoft.hospital.entity.PrescriptionAuditRecord;
import com.neusoft.hospital.entity.Register;
import com.neusoft.hospital.mapper.PrescriptionAuditRecordMapper;
import com.neusoft.hospital.service.DrugInfoService;
import com.neusoft.hospital.service.PrescriptionService;
import com.neusoft.hospital.service.RegisterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrescriptionAuditServiceImpl implements PrescriptionAuditService {

    private final PrescriptionService prescriptionService;
    private final RegisterService registerService;
    private final DrugInfoService drugInfoService;
    private final PrescriptionAuditClient prescriptionAuditClient;
    private final PrescriptionAuditRecordMapper prescriptionAuditRecordMapper;
    private final ObjectMapper objectMapper;

    @Override
    public PrescriptionAuditResultDTO audit(Integer registerId, boolean persist) {
        // 1. 聚合该挂号下全部处方明细行
        List<Prescription> prescriptions = prescriptionService.listByRegisterId(registerId);
        if (CollectionUtils.isEmpty(prescriptions)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "该挂号下无处方明细，无法审核");
        }

        // 2. 取药品信息（药名/规格），构造 drugId→DrugInfo 映射供富化
        List<Integer> drugIds = prescriptions.stream()
                .map(Prescription::getDrugId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Integer, DrugInfo> drugMap = drugIds.isEmpty()
                ? Collections.emptyMap()
                : drugInfoService.listByIds(drugIds).stream()
                .collect(Collectors.toMap(DrugInfo::getId, Function.identity()));

        // 3. 从 register 取患者性别/年龄
        PatientBrief patient = buildPatient(registerId);

        // 4. 装配发往 Python 的请求
        AiPrescriptionCheckRequest aiRequest = buildAiRequest(registerId, patient, prescriptions, drugMap);

        // 5. 调 Python AI（失败抛 BusinessException，直接向上冒泡，不落记录）
        AiPrescriptionCheckResponse aiResponse = prescriptionAuditClient.check(aiRequest);

        // 6. 富化结果（补药名）
        PrescriptionAuditResultDTO result = enrich(aiResponse, drugMap);

        // 7. 按需落 prescription_audit_record
        if (persist) {
            saveRecord(aiRequest, aiResponse, registerId);
        }
        return result;
    }

    @Override
    public List<PrescriptionAuditRecordDTO> listByRegisterId(Integer registerId) {
        LambdaQueryWrapper<PrescriptionAuditRecord> wrapper = new LambdaQueryWrapper<>();
        if (registerId != null) {
            wrapper.eq(PrescriptionAuditRecord::getRegisterId, registerId);
        }
        wrapper.orderByDesc(PrescriptionAuditRecord::getCreationTime);
        return prescriptionAuditRecordMapper.selectList(wrapper).stream()
                .map(this::toDTO)
                .toList();
    }

    private PrescriptionAuditRecordDTO toDTO(PrescriptionAuditRecord record) {
        PrescriptionAuditRecordDTO dto = new PrescriptionAuditRecordDTO();
        BeanUtils.copyProperties(record, dto);
        return dto;
    }

    private PatientBrief buildPatient(Integer registerId) {
        Register register = registerService.getById(registerId);
        if (register == null) {
            return null;
        }
        PatientBrief patient = new PatientBrief();
        patient.setAge(register.getAge());
        patient.setGender(register.getGender());
        return patient;
    }

    private AiPrescriptionCheckRequest buildAiRequest(Integer registerId,
                                                      PatientBrief patient,
                                                      List<Prescription> prescriptions,
                                                      Map<Integer, DrugInfo> drugMap) {
        AiPrescriptionCheckRequest aiRequest = new AiPrescriptionCheckRequest();
        aiRequest.setRegisterId(registerId);
        aiRequest.setPatient(patient); // 可空，Python 端 patient 为可选字段

        List<AiPrescriptionCheckRequest.DrugItem> drugs = prescriptions.stream().map(p -> {
            AiPrescriptionCheckRequest.DrugItem item = new AiPrescriptionCheckRequest.DrugItem();
            item.setDrugId(p.getDrugId());
            DrugInfo drug = p.getDrugId() == null ? null : drugMap.get(p.getDrugId());
            if (drug != null) {
                item.setDrugName(drug.getDrugName());
                item.setDrugFormat(drug.getDrugFormat());
            }
            item.setDrugUsage(p.getDrugUsage());
            item.setDrugNumber(p.getDrugNumber());
            return item;
        }).toList();
        aiRequest.setDrugs(drugs);
        return aiRequest;
    }

    private PrescriptionAuditResultDTO enrich(AiPrescriptionCheckResponse aiResponse,
                                              Map<Integer, DrugInfo> drugMap) {
        PrescriptionAuditResultDTO result = new PrescriptionAuditResultDTO();
        result.setRiskLevel(aiResponse.getRiskLevel());

        List<PrescriptionAuditResultDTO.SuggestionDTO> suggestions =
                (aiResponse.getSuggestions() == null ? Collections.<AiPrescriptionCheckResponse.Suggestion>emptyList()
                        : aiResponse.getSuggestions()).stream().map(s -> {
                    PrescriptionAuditResultDTO.SuggestionDTO dto = new PrescriptionAuditResultDTO.SuggestionDTO();
                    dto.setDrugId(s.getDrugId());
                    dto.setDrugName(nameOf(s.getDrugId(), drugMap));
                    dto.setContent(s.getContent());
                    return dto;
                }).toList();

        List<PrescriptionAuditResultDTO.InteractionDTO> interactions =
                (aiResponse.getInteractions() == null ? Collections.<AiPrescriptionCheckResponse.Interaction>emptyList()
                        : aiResponse.getInteractions()).stream().map(i -> {
                    PrescriptionAuditResultDTO.InteractionDTO dto = new PrescriptionAuditResultDTO.InteractionDTO();
                    dto.setDrugA(i.getDrugA());
                    dto.setDrugAName(nameOf(i.getDrugA(), drugMap));
                    dto.setDrugB(i.getDrugB());
                    dto.setDrugBName(nameOf(i.getDrugB(), drugMap));
                    dto.setLevel(i.getLevel());
                    dto.setDesc(i.getDesc());
                    return dto;
                }).toList();

        List<PrescriptionAuditResultDTO.RiskItemDTO> riskItems =
                (aiResponse.getRiskItems() == null ? Collections.<AiPrescriptionCheckResponse.RiskItem>emptyList()
                        : aiResponse.getRiskItems()).stream().map(r -> {
                    PrescriptionAuditResultDTO.RiskItemDTO dto = new PrescriptionAuditResultDTO.RiskItemDTO();
                    dto.setDrugId(r.getDrugId());
                    dto.setDrugName(nameOf(r.getDrugId(), drugMap));
                    dto.setType(r.getType());
                    dto.setDesc(r.getDesc());
                    return dto;
                }).toList();

        result.setSuggestions(suggestions);
        result.setInteractions(interactions);
        result.setRiskItems(riskItems);
        return result;
    }

    private String nameOf(Integer drugId, Map<Integer, DrugInfo> drugMap) {
        if (drugId == null) {
            return null;
        }
        DrugInfo drug = drugMap.get(drugId);
        return drug == null ? null : drug.getDrugName();
    }

    private void saveRecord(AiPrescriptionCheckRequest aiRequest,
                            AiPrescriptionCheckResponse aiResponse,
                            Integer registerId) {
        PrescriptionAuditRecord record = new PrescriptionAuditRecord();
        record.setRegisterId(registerId);
        record.setRequestSnapshot(toJson(aiRequest));
        record.setResultJson(toJson(aiResponse));
        record.setRiskLevel(aiResponse.getRiskLevel());
        record.setAuditorEmployeeId(CurrentUser.require());
        record.setCreationTime(LocalDateTime.now());
        prescriptionAuditRecordMapper.insert(record);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("序列化AI处方审核结果失败: {}", e.getMessage());
            return null;
        }
    }
}
