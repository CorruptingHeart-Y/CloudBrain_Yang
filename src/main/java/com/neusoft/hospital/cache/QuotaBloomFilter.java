package com.neusoft.hospital.cache;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.neusoft.hospital.entity.DoctorDailyQuota;
import com.neusoft.hospital.mapper.DoctorDailyQuotaMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuotaBloomFilter {

    private final DoctorDailyQuotaMapper quotaMapper;

    @Value("${hospital.registration.bloom-expected-insertions:100000}")
    private long expectedInsertions;

    @Value("${hospital.registration.bloom-fpp:0.01}")
    private double falsePositiveProbability;

    private volatile BloomFilter<String> bloomFilter;

    @PostConstruct
    public synchronized void init() {
        BloomFilter<String> initialized = BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8),
                expectedInsertions,
                falsePositiveProbability);

        try {
            List<DoctorDailyQuota> quotas = quotaMapper.selectList(new LambdaQueryWrapper<DoctorDailyQuota>()
                    .ge(DoctorDailyQuota::getQuotaDate, LocalDate.now()));
            for (DoctorDailyQuota quota : quotas) {
                initialized.put(buildKey(quota.getEmployeeId(), quota.getQuotaDate(), quota.getNoon()));
            }
            bloomFilter = initialized;
            log.info("Quota BloomFilter initialized, loaded {} future quota keys", quotas.size());
        } catch (Exception e) {
            bloomFilter = null;
            log.warn("Quota BloomFilter initialization failed, invalid quota filtering is bypassed: {}", e.getMessage());
        }
    }

    public boolean mightContain(Integer employeeId, LocalDate quotaDate, String noon) {
        BloomFilter<String> current = bloomFilter;
        return current == null || current.mightContain(buildKey(employeeId, quotaDate, noon));
    }

    public synchronized void put(Integer employeeId, LocalDate quotaDate, String noon) {
        if (bloomFilter == null || employeeId == null || quotaDate == null || noon == null) {
            return;
        }
        bloomFilter.put(buildKey(employeeId, quotaDate, noon));
    }

    private String buildKey(Integer employeeId, LocalDate quotaDate, String noon) {
        return employeeId + ":" + quotaDate + ":" + noon;
    }
}
