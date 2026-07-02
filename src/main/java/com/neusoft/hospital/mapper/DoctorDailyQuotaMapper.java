package com.neusoft.hospital.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neusoft.hospital.dto.response.PatientQuotaResponse;
import com.neusoft.hospital.entity.DoctorDailyQuota;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;

/**
 * 医生每日号源 Mapper。
 * <p>MySQL 兜底扣减：条件 UPDATE，affected rows=0 即满号。
 */
public interface DoctorDailyQuotaMapper extends BaseMapper<DoctorDailyQuota> {

    /**
     * 患者号源列表：联表 employee/department/regist_level，
     * 返回 [today, today+days] 区间内 remaining>0 的可抢号源。
     * <p>逻辑删除需手动过滤（@Select 不走 MyBatis-Plus 逻辑删除自动追加）。
     */
    @Select("SELECT q.employee_id AS employeeId, " +
            "       e.realname AS employeeName, " +
            "       e.deptment_id AS deptmentId, " +
            "       d.dept_name AS deptName, " +
            "       e.regist_level_id AS registLevelId, " +
            "       rl.regist_name AS registLevelName, " +
            "       rl.regist_fee AS registFee, " +
            "       q.quota_date AS quotaDate, " +
            "       q.noon AS noon, " +
            "       q.capacity AS capacity, " +
            "       q.remaining AS remaining " +
            "FROM doctor_daily_quota q " +
            "JOIN employee e ON e.id = q.employee_id AND e.delmark = 1 " +
            "JOIN department d ON d.id = e.deptment_id AND d.delmark = 1 " +
            "LEFT JOIN regist_level rl ON rl.id = e.regist_level_id AND rl.delmark = 1 " +
            "WHERE q.delmark = 1 " +
            "  AND q.remaining > 0 " +
            "  AND q.quota_date >= #{startDate} " +
            "  AND q.quota_date <= #{endDate} " +
            "ORDER BY q.quota_date ASC, q.noon ASC, e.deptment_id ASC, q.employee_id ASC")
    List<PatientQuotaResponse> listAvailableForPatient(@Param("startDate") LocalDate startDate,
                                                       @Param("endDate") LocalDate endDate);

    /**
     * 原子扣减一个号源（仅当 remaining>0 且未删除）。
     * @return 受影响行数：1=扣减成功；0=满号或行不存在
     */
    @Update("UPDATE doctor_daily_quota SET remaining = remaining - 1 " +
            "WHERE employee_id = #{employeeId} AND quota_date = #{quotaDate} " +
            "AND noon = #{noon} AND remaining > 0 AND delmark = 1")
    int deductIfAvailable(@Param("employeeId") Integer employeeId,
                          @Param("quotaDate") java.time.LocalDate quotaDate,
                          @Param("noon") String noon);

    /**
     * 原子回补一个号源（退号/抢号失败回退用），上限不超过 capacity。
     * @return 受影响行数
     */
    @Update("UPDATE doctor_daily_quota SET remaining = remaining + 1 " +
            "WHERE employee_id = #{employeeId} AND quota_date = #{quotaDate} " +
            "AND noon = #{noon} AND remaining < capacity AND delmark = 1")
    int refundOne(@Param("employeeId") Integer employeeId,
                  @Param("quotaDate") java.time.LocalDate quotaDate,
                  @Param("noon") String noon);
}
