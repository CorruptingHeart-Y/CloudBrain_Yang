package com.neusoft.hospital.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neusoft.hospital.ai.client.TriageClient;
import com.neusoft.hospital.ai.dto.AiTriageRequest;
import com.neusoft.hospital.ai.dto.AiTriageResponse;
import com.neusoft.hospital.ai.dto.TriageConsultRequest;
import com.neusoft.hospital.ai.dto.TriageResultDTO;
import com.neusoft.hospital.ai.service.TriageService;
import com.neusoft.hospital.auth.context.CurrentUser;
import com.neusoft.hospital.entity.Department;
import com.neusoft.hospital.entity.Employee;
import com.neusoft.hospital.entity.RegistLevel;
import com.neusoft.hospital.entity.TriageRecord;
import com.neusoft.hospital.mapper.TriageRecordMapper;
import com.neusoft.hospital.service.DepartmentService;
import com.neusoft.hospital.service.EmployeeService;
import com.neusoft.hospital.service.RegistLevelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TriageServiceImpl implements TriageService {

    private final DepartmentService departmentService;
    private final EmployeeService employeeService;
    private final RegistLevelService registLevelService;
    private final TriageClient triageClient;
    private final TriageRecordMapper triageRecordMapper;
    private final ObjectMapper objectMapper;

    @Override
    public TriageResultDTO consult(TriageConsultRequest request) {
        // 1. 装配候选集：全部科室 + 在岗医生（regist_level_id 非空）
        List<Department> deptList = departmentService.listAll();
        List<Employee> doctorList = employeeService.list(
                new LambdaQueryWrapper<Employee>().isNotNull(Employee::getRegistLevelId));

        Map<Integer, Department> deptMap = deptList.stream()
                .collect(Collectors.toMap(Department::getId, Function.identity()));
        Map<Integer, Employee> doctorMap = doctorList.stream()
                .collect(Collectors.toMap(Employee::getId, Function.identity()));
        Map<Integer, String> levelNameMap = registLevelService.listAll().stream()
                .collect(Collectors.toMap(RegistLevel::getId, RegistLevel::getRegistName));

        AiTriageRequest aiRequest = buildAiRequest(request, deptList, doctorList, levelNameMap);

        // 2. 调 Python AI（失败抛 BusinessException，直接向上冒泡，不落记录）
        AiTriageResponse aiResponse = triageClient.triage(aiRequest);

        // 3. 富化结果（补科室名/医生名/挂号级别名）
        TriageResultDTO result = enrich(aiResponse, deptMap, doctorMap, levelNameMap);

        // 4. 落 triage_record
        saveRecord(request, aiResponse, result);
        return result;
    }

    private AiTriageRequest buildAiRequest(TriageConsultRequest request,
                                           List<Department> deptList,
                                           List<Employee> doctorList,
                                           Map<Integer, String> levelNameMap) {
        AiTriageRequest aiRequest = new AiTriageRequest();
        aiRequest.setChiefComplaint(request.getChiefComplaint());
        aiRequest.setPatient(request.getPatient()); // 可空，Python 端 patient 为可选字段

        List<AiTriageRequest.CandidateDept> candidateDepts = deptList.stream().map(d -> {
            AiTriageRequest.CandidateDept c = new AiTriageRequest.CandidateDept();
            c.setDeptId(d.getId());
            c.setDeptName(d.getDeptName());
            c.setDeptType(d.getDeptType());
            return c;
        }).toList();
        List<AiTriageRequest.CandidateDoctor> candidateDoctors = doctorList.stream().map(e -> {
            AiTriageRequest.CandidateDoctor c = new AiTriageRequest.CandidateDoctor();
            c.setEmployeeId(e.getId());
            c.setDeptId(e.getDeptmentId());
            c.setRealname(e.getRealname());
            c.setRegistLevelName(e.getRegistLevelId() == null ? null : levelNameMap.get(e.getRegistLevelId()));
            return c;
        }).toList();
        aiRequest.setDepartments(candidateDepts);
        aiRequest.setDoctors(candidateDoctors);
        return aiRequest;
    }

    private TriageResultDTO enrich(AiTriageResponse aiResponse,
                                   Map<Integer, Department> deptMap,
                                   Map<Integer, Employee> doctorMap,
                                   Map<Integer, String> levelNameMap) {
        TriageResultDTO result = new TriageResultDTO();

        List<TriageResultDTO.RecommendedDeptDTO> deptDTOs =
                (aiResponse.getDepartments() == null ? Collections.<AiTriageResponse.RecommendedDept>emptyList()
                        : aiResponse.getDepartments()).stream().map(rd -> {
                    TriageResultDTO.RecommendedDeptDTO dto = new TriageResultDTO.RecommendedDeptDTO();
                    dto.setDeptId(rd.getDeptId());
                    Department d = rd.getDeptId() == null ? null : deptMap.get(rd.getDeptId());
                    dto.setDeptName(d == null ? null : d.getDeptName());
                    dto.setReason(rd.getReason());
                    dto.setScore(rd.getScore());
                    return dto;
                }).toList();

        List<TriageResultDTO.RecommendedDoctorDTO> doctorDTOs =
                (aiResponse.getDoctors() == null ? Collections.<AiTriageResponse.RecommendedDoctor>emptyList()
                        : aiResponse.getDoctors()).stream().map(rd -> {
                    TriageResultDTO.RecommendedDoctorDTO dto = new TriageResultDTO.RecommendedDoctorDTO();
                    dto.setEmployeeId(rd.getEmployeeId());
                    dto.setDeptId(rd.getDeptId());
                    Employee e = rd.getEmployeeId() == null ? null : doctorMap.get(rd.getEmployeeId());
                    dto.setRealname(e == null ? null : e.getRealname());
                    if (e != null && e.getRegistLevelId() != null) {
                        dto.setRegistLevelName(levelNameMap.get(e.getRegistLevelId()));
                    }
                    Department d = rd.getDeptId() == null ? null : deptMap.get(rd.getDeptId());
                    dto.setDeptName(d == null ? null : d.getDeptName());
                    dto.setReason(rd.getReason());
                    dto.setScore(rd.getScore());
                    return dto;
                }).toList();

        result.setDepartments(deptDTOs);
        result.setDoctors(doctorDTOs);
        return result;
    }

    private void saveRecord(TriageConsultRequest request, AiTriageResponse aiResponse, TriageResultDTO result) {
        TriageRecord record = new TriageRecord();
        record.setCardNumber(request.getCardNumber());
        record.setCaseNumber(request.getCaseNumber());
        record.setPatientName(request.getPatientName());
        if (request.getPatient() != null) {
            record.setGender(request.getPatient().getGender());
            record.setAge(request.getPatient().getAge());
        }
        record.setChiefComplaint(request.getChiefComplaint());
        record.setRecommendDeptIds(joinIds(result.getDepartments(), TriageResultDTO.RecommendedDeptDTO::getDeptId));
        record.setRecommendDoctorIds(joinIds(result.getDoctors(), TriageResultDTO.RecommendedDoctorDTO::getEmployeeId));
        record.setAiRawResult(toJson(aiResponse));
        record.setOperatorEmployeeId(CurrentUser.require());
        record.setCreationTime(LocalDateTime.now());
        triageRecordMapper.insert(record);
    }

    private <T> String joinIds(List<T> list, Function<T, Integer> idGetter) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.stream()
                .map(idGetter)
                .filter(java.util.Objects::nonNull)
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("序列化AI分诊结果失败: {}", e.getMessage());
            return null;
        }
    }
}
