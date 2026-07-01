package com.neusoft.hospital.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neusoft.hospital.ai.client.MedicalRecordClient;
import com.neusoft.hospital.ai.dto.AiMedicalRecordRequest;
import com.neusoft.hospital.ai.dto.AiMedicalRecordResponse;
import com.neusoft.hospital.ai.dto.MedicalRecordDraftDTO;
import com.neusoft.hospital.ai.dto.MedicalRecordDTO;
import com.neusoft.hospital.ai.dto.MedicalRecordSaveRequest;
import com.neusoft.hospital.ai.dto.PatientBrief;
import com.neusoft.hospital.ai.service.MedicalRecordService;
import com.neusoft.hospital.entity.Disease;
import com.neusoft.hospital.entity.MedicalRecord;
import com.neusoft.hospital.entity.MedicalRecordDisease;
import com.neusoft.hospital.entity.MedicalRecordMeta;
import com.neusoft.hospital.entity.Register;
import com.neusoft.hospital.mapper.MedicalRecordDiseaseMapper;
import com.neusoft.hospital.mapper.MedicalRecordMapper;
import com.neusoft.hospital.mapper.MedicalRecordMetaMapper;
import com.neusoft.hospital.service.DiseaseService;
import com.neusoft.hospital.service.RegisterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MedicalRecordServiceImpl implements MedicalRecordService {

    private static final String SOURCE_AI = "A";
    private static final String SOURCE_MANUAL = "M";

    private final MedicalRecordClient medicalRecordClient;
    private final MedicalRecordMapper medicalRecordMapper;
    private final MedicalRecordMetaMapper medicalRecordMetaMapper;
    private final MedicalRecordDiseaseMapper medicalRecordDiseaseMapper;
    private final RegisterService registerService;
    private final DiseaseService diseaseService;

    @Override
    public MedicalRecordDraftDTO generate(Integer registerId, String dialogue) {
        // 1. 从 register 取患者性别/年龄（辅助 AI 归纳，可空）
        PatientBrief patient = buildPatient(registerId);

        // 2. 装配发往 Python 的请求
        AiMedicalRecordRequest aiRequest = new AiMedicalRecordRequest();
        aiRequest.setRegisterId(registerId);
        aiRequest.setPatient(patient);
        aiRequest.setDialogue(dialogue);

        // 3. 调 Python AI（失败抛 BusinessException，不落库）
        AiMedicalRecordResponse aiResponse = medicalRecordClient.generate(aiRequest);

        // 4. 映射为草稿 DTO（纯文本，无需富化）
        MedicalRecordDraftDTO draft = new MedicalRecordDraftDTO();
        BeanUtils.copyProperties(aiResponse, draft);
        return draft;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MedicalRecordDTO save(MedicalRecordSaveRequest request) {
        // 1. upsert medical_record（register_id 唯一：存在则更新，否则新增）
        MedicalRecord record = medicalRecordMapper.selectOne(
                new LambdaQueryWrapper<MedicalRecord>()
                        .eq(MedicalRecord::getRegisterId, request.getRegisterId()));
        boolean isNew = record == null;
        if (isNew) {
            record = new MedicalRecord();
        }
        BeanUtils.copyProperties(request, record);
        // source 不进 medical_record — 走 medical_record_meta 新表
        if (isNew) {
            medicalRecordMapper.insert(record);
        } else {
            medicalRecordMapper.updateById(record);
        }

        // 2. upsert medical_record_meta（source + AI 快照）
        upsertMeta(record.getId(), request.getSource(), null, null);

        // 3. 替换疾病关联：先删旧、再插新（去重 + 过滤 null）
        replaceDiseaseLinks(record.getId(), request.getDiseaseIds());

        // 4. 回读（含疾病 + meta source）返前端
        return getByRegisterId(request.getRegisterId());
    }

    @Override
    public MedicalRecordDTO getByRegisterId(Integer registerId) {
        MedicalRecord record = medicalRecordMapper.selectOne(
                new LambdaQueryWrapper<MedicalRecord>()
                        .eq(MedicalRecord::getRegisterId, registerId));
        if (record == null) {
            return null;
        }
        MedicalRecordDTO dto = new MedicalRecordDTO();
        BeanUtils.copyProperties(record, dto);
        // source 从 meta 表取，无 meta 行默认 M
        MedicalRecordMeta meta = medicalRecordMetaMapper.selectOne(
                new LambdaQueryWrapper<MedicalRecordMeta>()
                        .eq(MedicalRecordMeta::getMedicalRecordId, record.getId()));
        dto.setSource(meta != null ? meta.getSource() : SOURCE_MANUAL);
        dto.setDiseases(loadDiseases(record.getId()));
        return dto;
    }

    // ---------- private helpers ----------

    private void upsertMeta(Integer medicalRecordId, String source,
                            String aiRequestSnapshot, String aiResultSnapshot) {
        String finalSource = StringUtils.hasText(source) ? source : SOURCE_MANUAL;
        MedicalRecordMeta meta = medicalRecordMetaMapper.selectOne(
                new LambdaQueryWrapper<MedicalRecordMeta>()
                        .eq(MedicalRecordMeta::getMedicalRecordId, medicalRecordId));
        if (meta == null) {
            meta = new MedicalRecordMeta();
            meta.setMedicalRecordId(medicalRecordId);
            meta.setSource(finalSource);
            meta.setAiRequestSnapshot(aiRequestSnapshot);
            meta.setAiResultSnapshot(aiResultSnapshot);
            meta.setCreateTime(LocalDateTime.now());
            medicalRecordMetaMapper.insert(meta);
        } else {
            meta.setSource(finalSource);
            if (aiRequestSnapshot != null) {
                meta.setAiRequestSnapshot(aiRequestSnapshot);
            }
            if (aiResultSnapshot != null) {
                meta.setAiResultSnapshot(aiResultSnapshot);
            }
            medicalRecordMetaMapper.updateById(meta);
        }
    }

    private void replaceDiseaseLinks(Integer medicalRecordId, List<Integer> diseaseIds) {
        medicalRecordDiseaseMapper.delete(
                new LambdaQueryWrapper<MedicalRecordDisease>()
                        .eq(MedicalRecordDisease::getMedicalRecordId, medicalRecordId));
        if (CollectionUtils.isEmpty(diseaseIds)) {
            return;
        }
        LinkedHashSet<Integer> uniqueIds = diseaseIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (Integer diseaseId : uniqueIds) {
            MedicalRecordDisease link = new MedicalRecordDisease();
            link.setMedicalRecordId(medicalRecordId);
            link.setDiseaseId(diseaseId);
            medicalRecordDiseaseMapper.insert(link);
        }
    }

    private List<MedicalRecordDTO.DiseaseSimpleDTO> loadDiseases(Integer medicalRecordId) {
        List<Integer> diseaseIds = medicalRecordDiseaseMapper.selectList(
                        new LambdaQueryWrapper<MedicalRecordDisease>()
                                .eq(MedicalRecordDisease::getMedicalRecordId, medicalRecordId)
                                .orderByAsc(MedicalRecordDisease::getDiseaseId))
                .stream()
                .map(MedicalRecordDisease::getDiseaseId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (diseaseIds.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Integer, Disease> diseaseMap = diseaseService.listByIds(diseaseIds).stream()
                .collect(Collectors.toMap(Disease::getId, Function.identity()));
        return diseaseIds.stream()
                .map(diseaseMap::get)
                .filter(Objects::nonNull)
                .map(d -> {
                    MedicalRecordDTO.DiseaseSimpleDTO dto = new MedicalRecordDTO.DiseaseSimpleDTO();
                    dto.setId(d.getId());
                    dto.setDiseaseName(d.getDiseaseName());
                    dto.setDiseaseICD(d.getDiseaseICD());
                    return dto;
                })
                .toList();
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
}
