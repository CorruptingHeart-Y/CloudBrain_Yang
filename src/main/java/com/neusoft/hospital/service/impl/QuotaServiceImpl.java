package com.neusoft.hospital.service.impl;

import com.neusoft.hospital.common.BusinessException;
import com.neusoft.hospital.common.ErrorCode;
import com.neusoft.hospital.entity.DoctorDailyQuota;
import com.neusoft.hospital.mapper.DoctorDailyQuotaMapper;
import com.neusoft.hospital.service.DoctorDailyQuotaService;
import com.neusoft.hospital.service.QuotaService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuotaServiceImpl implements QuotaService {

    private final StringRedisTemplate redis;
    private final DoctorDailyQuotaService quotaService;
    private final DoctorDailyQuotaMapper quotaMapper;

    @Value("${hospital.registration.stock-key-prefix:regist:stock}")
    private String stockKeyPrefix;
    @Value("${hospital.registration.stock-ttl-hours:48}")
    private long stockTtlHours;

    private DefaultRedisScript<Long> deductScript;

    @PostConstruct
    void initScript() {
        deductScript = new DefaultRedisScript<>();
        deductScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/deduct_stock.lua")));
        deductScript.setResultType(Long.class);
    }

    private String key(Integer employeeId, LocalDate date, String noon) {
        return stockKeyPrefix + ":" + employeeId + ":" + date + ":" + noon;
    }
    //执行lua脚本的并判断是否在redis扣减成功.
    @Override
    public int tryDeduct(Integer employeeId, LocalDate quotaDate, String noon) {
        String k = key(employeeId, quotaDate, noon);

        Long r = redis.execute(deductScript, List.of(k));
        if (r == null) {
            return -1;
        }
        int result = r.intValue();
        if (result == -1) {
            // 库存 key 未初始化：lazy seed 后重试一次

            // 返回-1,可能是因为数据库有但是redis没有缓存, 尝试加载到redis,如果加载成功就再做一次lua脚本原子扣减.
            if (seedIfPresent(employeeId, quotaDate, noon)) {
                Long r2 = redis.execute(deductScript, List.of(k));
                return r2 == null ? -1 : r2.intValue();
            }
            //如果数据库页没有,说明压根就没号直接返回-1
            return -1; // DB 也无号源行 → 未放号
        }
        return result;
    }

    /** DB 有号源行则 SET Redis 库存并返回 true；无则 false。 */
    private boolean seedIfPresent(Integer employeeId, LocalDate quotaDate, String noon) {
        DoctorDailyQuota q = quotaService.getByEmpDateNoon(employeeId, quotaDate, noon);
        if (q == null) {
            return false;
        }
        String k = key(employeeId, quotaDate, noon);
        Boolean seeded = redis.opsForValue().setIfAbsent(
                k, String.valueOf(q.getRemaining()), Duration.ofHours(stockTtlHours));
        if (Boolean.FALSE.equals(seeded)) {
            log.debug("Redis 号源已由其他请求完成懒加载 key={}", k);
        }
        return true;
    }

    @Override
    public void refundRedis(Integer employeeId, LocalDate quotaDate, String noon) {
        try {
            String k = key(employeeId, quotaDate, noon);
            redis.opsForValue().increment(k);
            redis.expire(k, Duration.ofHours(stockTtlHours));
        } catch (Exception e) {
            log.warn("refundRedis 失败 emp={} date={} noon={}: {}", employeeId, quotaDate, noon, e.getMessage());
        }
    }

    @Override
    public void seedRedis(Integer employeeId, LocalDate quotaDate, String noon) {
        DoctorDailyQuota q = quotaService.getByEmpDateNoon(employeeId, quotaDate, noon);
        if (q == null) {
            return;
        }
        redis.opsForValue().set(
                key(employeeId, quotaDate, noon),
                String.valueOf(q.getRemaining()),
                Duration.ofHours(stockTtlHours));
    }

    @Override
    public void decrRedisIfPresent(Integer employeeId, LocalDate quotaDate, String noon) {
        String k = key(employeeId, quotaDate, noon);
        try {
            if (Boolean.TRUE.equals(redis.hasKey(k))) {
                redis.opsForValue().decrement(k);
            }
        } catch (Exception e) {
            log.warn("decrRedisIfPresent 失败 emp={} date={} noon={}: {}", employeeId, quotaDate, noon, e.getMessage());
        }
    }

    @Override
    //真实的落库业务方法.
    public boolean deductDbOrThrow(Integer employeeId, LocalDate quotaDate, String noon) {
        int affected = quotaMapper.deductIfAvailable(employeeId, quotaDate, noon);
        if (affected == 1) {
            //确实mysql落库成功.
            return true;
        }
        // affected=0：可能无号源行(未放号) 或 remaining=0(满号)
        DoctorDailyQuota q = quotaService.getByEmpDateNoon(employeeId, quotaDate, noon);
        if (q == null) {
            return false; // 未放号，现场路径放行
        }
        // 已放号但满号 → 409
        throw new BusinessException(ErrorCode.CONFLICT.getCode(), "该号源已约满");
    }

    @Override
    public DoctorDailyQuota getQuota(Integer employeeId, LocalDate quotaDate, String noon) {
        return quotaService.getByEmpDateNoon(employeeId, quotaDate, noon);
    }
}
