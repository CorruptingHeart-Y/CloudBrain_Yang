package com.neusoft.hospital.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.neusoft.hospital.dto.response.PatientQuotaResponse;
import com.neusoft.hospital.entity.DoctorDailyQuota;

import java.time.LocalDate;
import java.util.List;

public interface DoctorDailyQuotaService extends IService<DoctorDailyQuota> {

    /** 按医生+日期+午别查号源（未放号返回 null）。 */
    DoctorDailyQuota getByEmpDateNoon(Integer employeeId, LocalDate quotaDate, String noon);

    /** upsert 号源：存在则更新 capacity 并重置 remaining=capacity，否则新建。返回落库后的实体。 */
    DoctorDailyQuota upsert(Integer employeeId, LocalDate quotaDate, String noon, Integer capacity);

    /** 查询某医生某日所有午别号源。 */
    List<DoctorDailyQuota> listByEmpDate(Integer employeeId, LocalDate quotaDate);

    /**
     * 患者号源列表：返回 [今天, 今天+days] 区间内 remaining>0 的可抢号源，
     * 联表带出医生姓名/科室/级别名称等展示字段。
     * @param days 天数，建议 1~30
     */
    List<PatientQuotaResponse> listAvailableForPatient(int days);
}
