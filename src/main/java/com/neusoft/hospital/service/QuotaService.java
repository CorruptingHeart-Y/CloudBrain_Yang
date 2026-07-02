package com.neusoft.hospital.service;

import com.neusoft.hospital.entity.DoctorDailyQuota;

import java.time.LocalDate;

/**
 * 号源库存服务：Redis Lua 原子扣减 + MySQL 兜底。
 * <p>Redis 仅为快速过滤缓存，DB {@code doctor_daily_quota.remaining} 为权威库存。
 */
public interface QuotaService {

    /**
     * Redis Lua 原子扣减（抢号快速路径）。
     * @return 1=扣减成功；0=已约满；-1=未放号(key 未初始化且 DB 无号源行)
     */
    int tryDeduct(Integer employeeId, LocalDate quotaDate, String noon);

    /** 回补一个号源到 Redis（消费失败/退号时调用）。 */
    void refundRedis(Integer employeeId, LocalDate quotaDate, String noon);

    /** 现场挂号扣 DB 后，若 Redis 库存 key 存在则同步 DECR（best-effort 防漂移）。 */
    void decrRedisIfPresent(Integer employeeId, LocalDate quotaDate, String noon);

    /** 放号/重置时把 DB remaining 灌入 Redis。 */
    void seedRedis(Integer employeeId, LocalDate quotaDate, String noon);

    /**
     * MySQL 兜底扣减（消费端权威路径，与 register 落库同事务）。
     * @return true=扣减成功；false=无号源行(未放号,现场路径放行) 或 满号
     * @throws com.neusoft.hospital.common.BusinessException(409) 当号源行存在但 remaining=0
     */
    boolean deductDbOrThrow(Integer employeeId, LocalDate quotaDate, String noon);

    /** 查号源（含 remaining），未放号返回 null。 */
    DoctorDailyQuota getQuota(Integer employeeId, LocalDate quotaDate, String noon);
}
