package com.neusoft.hospital.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.hospital.auth.context.AuthUser;
import com.neusoft.hospital.auth.context.CurrentUser;
import com.neusoft.hospital.common.BusinessException;
import com.neusoft.hospital.common.ErrorCode;
import com.neusoft.hospital.common.PageResult;
import com.neusoft.hospital.dto.response.PatientProfileResponse;
import com.neusoft.hospital.dto.response.PatientRecordDetailResponse;
import com.neusoft.hospital.dto.response.PatientRecordSummaryResponse;
import com.neusoft.hospital.entity.CheckRequest;
import com.neusoft.hospital.entity.Department;
import com.neusoft.hospital.entity.DisposalRequest;
import com.neusoft.hospital.entity.Employee;
import com.neusoft.hospital.entity.InspectionRequest;
import com.neusoft.hospital.entity.MedicalRecord;
import com.neusoft.hospital.entity.Prescription;
import com.neusoft.hospital.entity.Register;
import com.neusoft.hospital.entity.RegistLevel;
import com.neusoft.hospital.mapper.MedicalRecordMapper;
import com.neusoft.hospital.mapper.PatientMapper;
import com.neusoft.hospital.service.CheckRequestService;
import com.neusoft.hospital.service.DepartmentService;
import com.neusoft.hospital.service.DisposalRequestService;
import com.neusoft.hospital.service.EmployeeService;
import com.neusoft.hospital.service.InspectionRequestService;
import com.neusoft.hospital.service.PatientPortalService;
import com.neusoft.hospital.service.PatientRegisterLinkService;
import com.neusoft.hospital.service.PrescriptionService;
import com.neusoft.hospital.service.RegistLevelService;
import com.neusoft.hospital.service.RegisterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 患者门户服务实现（v2.0）。
 * <p>
 * 安全要点：
 * <ul>
 *   <li>patientId 只取自 {@link CurrentUser}（来源为已签名校验的 JWT v2），不接收前端入参；</li>
 *   <li>患者↔挂号归属经 patient_register_link 桥接表表达，不依赖 register.patient_id；</li>
 *   <li>列表查询：先取 link 内 register_id，再查 register；未 link 的历史记录天然排除；</li>
 *   <li>详情查询：先校验 link 存在，命中后才读 register 与派生表；不使用 card_number 兜底。</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class PatientPortalServiceImpl implements PatientPortalService {

    private final PatientMapper patientMapper;
    private final RegisterService registerService;
    private final PatientRegisterLinkService patientRegisterLinkService;
    private final MedicalRecordMapper medicalRecordMapper;
    private final PrescriptionService prescriptionService;
    private final CheckRequestService checkRequestService;
    private final InspectionRequestService inspectionRequestService;
    private final DisposalRequestService disposalRequestService;
    private final DepartmentService departmentService;
    private final EmployeeService employeeService;
    private final RegistLevelService registLevelService;

    /** 取当前患者 ID；缺失视为账号关联异常 → 401（不得 NPE）。 */
    private Integer currentPatientId() {
        AuthUser user = CurrentUser.requireAuthUser();
        Integer pid = user.getPatientId();
        if (pid == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return pid;
    }

    @Override
    public PatientProfileResponse getCurrentPatientProfile() {
        Integer pid = currentPatientId();
        com.neusoft.hospital.entity.Patient patient = patientMapper.selectById(pid);
        if (patient == null) {
            // 账号关联异常：不透露记录是否存在，按 401 语义返回
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        PatientProfileResponse resp = new PatientProfileResponse();
        resp.setPatientId(pid);
        resp.setRealName(patient.getRealName());
        resp.setGender(patient.getGender());
        resp.setBirthdate(patient.getBirthdate());
        // 刻意不设置 cardNumber / phone / homeAddress
        return resp;
    }

    @Override
    public PageResult<PatientRecordSummaryResponse> pageCurrentPatientRecords(Integer pageNum, Integer pageSize) {
        Integer pid = currentPatientId();
        int pNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int pSize = pageSize == null || pageSize < 1 ? 10 : pageSize;

        // 经桥接表取当前患者的 register_id；未 link 的历史记录天然排除
        List<Integer> registerIds = patientRegisterLinkService.findRegisterIdsByPatient(pid);
        if (registerIds.isEmpty()) {
            return PageResult.of(0L, pNum, pSize, List.of());
        }

        Page<Register> page = new Page<>(pNum, pSize);
        LambdaQueryWrapper<Register> wrapper = new LambdaQueryWrapper<Register>()
                .in(Register::getId, registerIds)
                .orderByDesc(Register::getId);
        IPage<Register> result = registerService.page(page, wrapper);

        List<PatientRecordSummaryResponse> records = result.getRecords().stream()
                .map(this::toSummary)
                .toList();
        return PageResult.of(result.getTotal(), pNum, pSize, records);
    }

    @Override
    public PatientRecordDetailResponse getCurrentPatientRecordDetail(Integer registerId) {
        Integer pid = currentPatientId();
        if (registerId == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        // 归属校验：经桥接表校验 (patientId, registerId) 是否存在 link，命中前不读 register
        if (!patientRegisterLinkService.existsLink(pid, registerId)) {
            // 不属于当前患者的 registerId（含不存在/未 link）→ 404，不透露记录是否存在
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        Register register = registerService.getById(registerId);
        if (register == null) {
            // link 存在但 register 被物理删除的异常情况
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        PatientRecordDetailResponse resp = new PatientRecordDetailResponse();
        resp.setRegister(toRegisterSummary(register));

        // 病历：medical_record.register_id 唯一，0 或 1 条
        MedicalRecord mr = medicalRecordMapper.selectOne(
                new LambdaQueryWrapper<MedicalRecord>().eq(MedicalRecord::getRegisterId, registerId));
        resp.setMedicalRecord(mr == null ? null : toMedicalRecordSummary(mr));

        // 关联列表：仅以已验证的 registerId 查询，缺失即空列表
        List<Prescription> prescriptions = prescriptionService.listByRegisterId(registerId);
        resp.setPrescriptions(prescriptions.stream().map(this::toPrescriptionItem).toList());

        List<CheckRequest> checks = checkRequestService.listByRegisterId(registerId);
        resp.setCheckRequests(checks.stream().map(this::toCheckItem).toList());

        List<InspectionRequest> inspections = inspectionRequestService.listByRegisterId(registerId);
        resp.setInspectionRequests(inspections.stream().map(this::toInspectionItem).toList());

        List<DisposalRequest> disposals = disposalRequestService.listByRegisterId(registerId);
        resp.setDisposalRequests(disposals.stream().map(this::toDisposalItem).toList());

        return resp;
    }

    // ---------- 映射 ----------

    private PatientRecordSummaryResponse toSummary(Register r) {
        PatientRecordSummaryResponse s = new PatientRecordSummaryResponse();
        s.setRegisterId(r.getId());
        s.setCaseNumber(r.getCaseNumber());
        s.setVisitDate(r.getVisitDate());
        s.setNoon(r.getNoon());
        s.setDeptName(resolveDeptName(r.getDeptmentId()));
        s.setEmployeeName(resolveEmployeeName(r.getEmployeeId()));
        s.setRegistLevelName(resolveRegistLevelName(r.getRegistLevelId()));
        s.setVisitState(r.getVisitState());
        return s;
    }

    private PatientRecordDetailResponse.RegisterSummary toRegisterSummary(Register r) {
        PatientRecordDetailResponse.RegisterSummary s = new PatientRecordDetailResponse.RegisterSummary();
        s.setRegisterId(r.getId());
        s.setCaseNumber(r.getCaseNumber());
        s.setVisitDate(r.getVisitDate());
        s.setNoon(r.getNoon());
        s.setDeptName(resolveDeptName(r.getDeptmentId()));
        s.setEmployeeName(resolveEmployeeName(r.getEmployeeId()));
        s.setRegistLevelName(resolveRegistLevelName(r.getRegistLevelId()));
        s.setVisitState(r.getVisitState());
        return s;
    }

    private PatientRecordDetailResponse.MedicalRecordSummary toMedicalRecordSummary(MedicalRecord mr) {
        PatientRecordDetailResponse.MedicalRecordSummary s = new PatientRecordDetailResponse.MedicalRecordSummary();
        s.setReadme(mr.getReadme());
        s.setPresent(mr.getPresent());
        s.setPresentTreat(mr.getPresentTreat());
        s.setHistory(mr.getHistory());
        s.setAllergy(mr.getAllergy());
        s.setPhysique(mr.getPhysique());
        s.setProposal(mr.getProposal());
        s.setCareful(mr.getCareful());
        s.setDiagnosis(mr.getDiagnosis());
        s.setCure(mr.getCure());
        return s;
    }

    private PatientRecordDetailResponse.PrescriptionItem toPrescriptionItem(Prescription p) {
        PatientRecordDetailResponse.PrescriptionItem i = new PatientRecordDetailResponse.PrescriptionItem();
        i.setId(p.getId());
        i.setDrugId(p.getDrugId());
        i.setDrugUsage(p.getDrugUsage());
        i.setDrugNumber(p.getDrugNumber());
        i.setCreationTime(p.getCreationTime());
        i.setDrugState(p.getDrugState());
        return i;
    }

    private PatientRecordDetailResponse.CheckItem toCheckItem(CheckRequest c) {
        PatientRecordDetailResponse.CheckItem i = new PatientRecordDetailResponse.CheckItem();
        i.setId(c.getId());
        i.setCheckInfo(c.getCheckInfo());
        i.setCheckPosition(c.getCheckPosition());
        i.setCreationTime(c.getCreationTime());
        i.setCheckTime(c.getCheckTime());
        i.setCheckResult(c.getCheckResult());
        i.setCheckState(c.getCheckState());
        i.setCheckRemark(c.getCheckRemark());
        return i;
    }

    private PatientRecordDetailResponse.InspectionItem toInspectionItem(InspectionRequest n) {
        PatientRecordDetailResponse.InspectionItem i = new PatientRecordDetailResponse.InspectionItem();
        i.setId(n.getId());
        i.setInspectionInfo(n.getInspectionInfo());
        i.setInspectionPosition(n.getInspectionPosition());
        i.setCreationTime(n.getCreationTime());
        i.setInspectionTime(n.getInspectionTime());
        i.setInspectionResult(n.getInspectionResult());
        i.setInspectionState(n.getInspectionState());
        i.setInspectionRemark(n.getInspectionRemark());
        return i;
    }

    private PatientRecordDetailResponse.DisposalItem toDisposalItem(DisposalRequest d) {
        PatientRecordDetailResponse.DisposalItem i = new PatientRecordDetailResponse.DisposalItem();
        i.setId(d.getId());
        i.setDisposalInfo(d.getDisposalInfo());
        i.setDisposalPosition(d.getDisposalPosition());
        i.setCreationTime(d.getCreationTime());
        i.setDisposalTime(d.getDisposalTime());
        i.setDisposalResult(d.getDisposalResult());
        i.setDisposalState(d.getDisposalState());
        i.setDisposalRemark(d.getDisposalRemark());
        return i;
    }

    private String resolveDeptName(Integer deptmentId) {
        if (deptmentId == null) return null;
        Department dept = departmentService.getById(deptmentId);
        return dept == null ? null : dept.getDeptName();
    }

    private String resolveEmployeeName(Integer employeeId) {
        if (employeeId == null) return null;
        Employee emp = employeeService.getById(employeeId);
        return emp == null ? null : emp.getRealname();
    }

    private String resolveRegistLevelName(Integer registLevelId) {
        if (registLevelId == null) return null;
        RegistLevel level = registLevelService.getById(registLevelId);
        return level == null ? null : level.getRegistName();
    }
}
