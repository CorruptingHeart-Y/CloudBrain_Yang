package com.neusoft.hospital.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neusoft.hospital.dto.response.PatientQuotaResponse;
import com.neusoft.hospital.entity.DoctorDailyQuota;
import com.neusoft.hospital.mapper.DoctorDailyQuotaMapper;
import com.neusoft.hospital.service.DoctorDailyQuotaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DoctorDailyQuotaServiceImpl extends ServiceImpl<DoctorDailyQuotaMapper, DoctorDailyQuota>
        implements DoctorDailyQuotaService {

    @Override
    public DoctorDailyQuota getByEmpDateNoon(Integer employeeId, LocalDate quotaDate, String noon) {
        return this.getOne(new LambdaQueryWrapper<DoctorDailyQuota>()
                .eq(DoctorDailyQuota::getEmployeeId, employeeId)
                .eq(DoctorDailyQuota::getQuotaDate, quotaDate)
                .eq(DoctorDailyQuota::getNoon, noon));
    }

    @Override
    public DoctorDailyQuota upsert(Integer employeeId, LocalDate quotaDate, String noon, Integer capacity) {
        DoctorDailyQuota q = getByEmpDateNoon(employeeId, quotaDate, noon);
        if (q == null) {
            q = new DoctorDailyQuota();
            q.setEmployeeId(employeeId);
            q.setQuotaDate(quotaDate);
            q.setNoon(noon);
            q.setCapacity(capacity);
            q.setRemaining(capacity);
            this.save(q);
        } else {
            q.setCapacity(capacity);
            q.setRemaining(capacity); // 放号/重置：remaining 回满
            this.updateById(q);
        }
        return q;
    }

    @Override
    public List<DoctorDailyQuota> listByEmpDate(Integer employeeId, LocalDate quotaDate) {
        return this.list(new LambdaQueryWrapper<DoctorDailyQuota>()
                .eq(DoctorDailyQuota::getEmployeeId, employeeId)
                .eq(DoctorDailyQuota::getQuotaDate, quotaDate)
                .orderByAsc(DoctorDailyQuota::getNoon));
    }

    @Override
    public List<PatientQuotaResponse> listAvailableForPatient(int days) {
        int safeDays = Math.min(Math.max(days, 1), 30); // 钳制到 1~30，防止滥用
        LocalDate today = LocalDate.now();
        return baseMapper.listAvailableForPatient(today, today.plusDays(safeDays));
    }
}
