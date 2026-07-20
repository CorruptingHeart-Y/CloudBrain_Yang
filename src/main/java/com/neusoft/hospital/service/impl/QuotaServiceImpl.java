package com.neusoft.hospital.service.impl;

import com.neusoft.hospital.cache.QuotaBloomFilter;
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
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuotaServiceImpl implements QuotaService {

    private static final String RELEASE_LOCK_SCRIPT =
            "if redis.call('GET', KEYS[1]) == ARGV[1] then " +
                    "return redis.call('DEL', KEYS[1]) " +
                    "else return 0 end";

    private final StringRedisTemplate redis;
    private final DoctorDailyQuotaService quotaService;
    private final DoctorDailyQuotaMapper quotaMapper;
    private final QuotaBloomFilter quotaBloomFilter;

    @Value("${hospital.registration.stock-key-prefix:regist:stock}")
    private String stockKeyPrefix;

    @Value("${hospital.registration.stock-ttl-hours:48}")
    private long stockTtlHours;

    @Value("${hospital.registration.stock-ttl-jitter-minutes:30}")
    private long stockTtlJitterMinutes;

    private DefaultRedisScript<Long> deductScript;
    private DefaultRedisScript<Long> releaseLockScript;

    @PostConstruct
    void initScript() {
        deductScript = new DefaultRedisScript<>();
        deductScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/deduct_stock.lua")));
        deductScript.setResultType(Long.class);

        releaseLockScript = new DefaultRedisScript<>();
        releaseLockScript.setScriptText(RELEASE_LOCK_SCRIPT);
        releaseLockScript.setResultType(Long.class);
    }

    private String key(Integer employeeId, LocalDate date, String noon) {
        return stockKeyPrefix + ":" + employeeId + ":" + date + ":" + noon;
    }

    private String lockKey(Integer employeeId, LocalDate date, String noon) {
        return stockKeyPrefix + ":lock:" + employeeId + ":" + date + ":" + noon;
    }

    @Override
    public int tryDeduct(Integer employeeId, LocalDate quotaDate, String noon) {
        if (!quotaBloomFilter.mightContain(employeeId, quotaDate, noon)) {
            return -1;
        }

        String k = key(employeeId, quotaDate, noon);
        Long r = redis.execute(deductScript, List.of(k));
        if (r == null) {
            return -1;
        }

        int result = r.intValue();
        if (result == -1) {
            if (rebuildStockCacheWithMutex(employeeId, quotaDate, noon)) {
                Long r2 = redis.execute(deductScript, List.of(k));
                return r2 == null ? -1 : r2.intValue();
            }
            return -1;
        }
        return result;
    }

    private boolean seedIfPresent(Integer employeeId, LocalDate quotaDate, String noon) {
        DoctorDailyQuota q = quotaService.getByEmpDateNoon(employeeId, quotaDate, noon);
        if (q == null) {
            return false;
        }

        quotaBloomFilter.put(employeeId, quotaDate, noon);
        String k = key(employeeId, quotaDate, noon);
        Boolean seeded = redis.opsForValue().setIfAbsent(
                k, String.valueOf(q.getRemaining()), stockTtlWithJitter());
        if (Boolean.FALSE.equals(seeded)) {
            log.debug("Redis stock cache already rebuilt by another request, key={}", k);
        }
        return true;
    }

    private boolean rebuildStockCacheWithMutex(Integer employeeId, LocalDate quotaDate, String noon) {
        String lockKey = lockKey(employeeId, quotaDate, noon);
        String lockValue = UUID.randomUUID().toString();

        Boolean locked = redis.opsForValue().setIfAbsent(lockKey, lockValue, Duration.ofSeconds(5));
        if (Boolean.TRUE.equals(locked)) {
            try {
                return seedIfPresent(employeeId, quotaDate, noon);
            } finally {
                releaseLockSafely(lockKey, lockValue);
            }
        }

        sleepQuietly(30);
        return true;
    }

    private void releaseLockSafely(String lockKey, String lockValue) {
        try {
            redis.execute(releaseLockScript, List.of(lockKey), lockValue);
        } catch (Exception e) {
            log.warn("release stock rebuild lock failed key={}: {}", lockKey, e.getMessage());
        }
    }

    @Override
    public void refundRedis(Integer employeeId, LocalDate quotaDate, String noon) {
        try {
            String k = key(employeeId, quotaDate, noon);
            redis.opsForValue().increment(k);
            redis.expire(k, stockTtlWithJitter());
        } catch (Exception e) {
            log.warn("refundRedis failed emp={} date={} noon={}: {}", employeeId, quotaDate, noon, e.getMessage());
        }
    }

    @Override
    public void seedRedis(Integer employeeId, LocalDate quotaDate, String noon) {
        DoctorDailyQuota q = quotaService.getByEmpDateNoon(employeeId, quotaDate, noon);
        if (q == null) {
            return;
        }

        quotaBloomFilter.put(employeeId, quotaDate, noon);
        redis.opsForValue().set(
                key(employeeId, quotaDate, noon),
                String.valueOf(q.getRemaining()),
                stockTtlWithJitter());
    }

    @Override
    public void decrRedisIfPresent(Integer employeeId, LocalDate quotaDate, String noon) {
        String k = key(employeeId, quotaDate, noon);
        try {
            if (Boolean.TRUE.equals(redis.hasKey(k))) {
                redis.opsForValue().decrement(k);
            }
        } catch (Exception e) {
            log.warn("decrRedisIfPresent failed emp={} date={} noon={}: {}", employeeId, quotaDate, noon, e.getMessage());
        }
    }

    @Override
    public boolean deductDbOrThrow(Integer employeeId, LocalDate quotaDate, String noon) {
        int affected = quotaMapper.deductIfAvailable(employeeId, quotaDate, noon);
        if (affected == 1) {
            return true;
        }

        DoctorDailyQuota q = quotaService.getByEmpDateNoon(employeeId, quotaDate, noon);
        if (q == null) {
            return false;
        }
        throw new BusinessException(ErrorCode.CONFLICT.getCode(), "\u8be5\u53f7\u6e90\u5df2\u7ea6\u6ee1");
    }

    @Override
    public DoctorDailyQuota getQuota(Integer employeeId, LocalDate quotaDate, String noon) {
        return quotaService.getByEmpDateNoon(employeeId, quotaDate, noon);
    }

    private Duration stockTtlWithJitter() {
        long safeTtlHours = Math.max(stockTtlHours, 1);
        long safeJitterMinutes = Math.max(stockTtlJitterMinutes, 0);
        long jitterMinutes = safeJitterMinutes == 0
                ? 0
                : ThreadLocalRandom.current().nextLong(safeJitterMinutes + 1);
        return Duration.ofHours(safeTtlHours).plusMinutes(jitterMinutes);
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
