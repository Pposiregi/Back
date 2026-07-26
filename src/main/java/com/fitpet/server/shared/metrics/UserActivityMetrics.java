package com.fitpet.server.shared.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserActivityMetrics {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");
    private static final Duration DAILY_RETENTION = Duration.ofDays(40);
    private static final Duration MONTHLY_WINDOW = Duration.ofDays(30);
    private static final String DAILY_KEY_PREFIX = "analytics:active-users:daily:";
    private static final String LAST_SEEN_KEY = "analytics:active-users:last-seen";

    private final StringRedisTemplate redisTemplate;
    private final AtomicLong dailyActiveUsers = new AtomicLong();
    private final AtomicLong monthlyActiveUsers = new AtomicLong();

    public UserActivityMetrics(StringRedisTemplate redisTemplate, MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        registerGauge(meterRegistry, "daily", dailyActiveUsers);
        registerGauge(meterRegistry, "monthly", monthlyActiveUsers);
    }

    public void recordActiveUser(Long userId) {
        if (userId == null) {
            return;
        }

        ZonedDateTime now = ZonedDateTime.now(SERVICE_ZONE);
        addDailyActiveUser(dailyKey(now.toLocalDate()), userId);
        updateLastSeen(userId, now);
    }

    @Scheduled(
            fixedDelayString = "${analytics.metrics.active-users-refresh-ms:60000}",
            initialDelayString = "${analytics.metrics.initial-delay-ms:10000}"
    )
    void refreshActiveUserGauges() {
        ZonedDateTime now = ZonedDateTime.now(SERVICE_ZONE);
        try {
            dailyActiveUsers.set(sizeOf(dailyKey(now.toLocalDate())));
            double cutoff = now.minus(MONTHLY_WINDOW).toEpochSecond();
            redisTemplate.opsForZSet().removeRangeByScore(LAST_SEEN_KEY, 0, cutoff);
            monthlyActiveUsers.set(zSetSize(LAST_SEEN_KEY));
        } catch (RuntimeException e) {
            log.warn("[UserActivityMetrics] 활성 사용자 지표 갱신 실패", e);
        }
    }

    private void addDailyActiveUser(String key, Long userId) {
        try {
            Long added = redisTemplate.opsForSet().add(key, String.valueOf(userId));
            if (Long.valueOf(1L).equals(added)) {
                redisTemplate.expire(key, DAILY_RETENTION);
            }
        } catch (RuntimeException e) {
            log.warn("[UserActivityMetrics] 활성 사용자 기록 실패: key={}, userId={}", key, userId, e);
        }
    }

    private void updateLastSeen(Long userId, ZonedDateTime now) {
        try {
            redisTemplate.opsForZSet().add(LAST_SEEN_KEY, String.valueOf(userId), now.toEpochSecond());
        } catch (RuntimeException e) {
            log.warn("[UserActivityMetrics] 최근 활동 기록 실패: userId={}", userId, e);
        }
    }

    private long sizeOf(String key) {
        Long size = redisTemplate.opsForSet().size(key);
        return size == null ? 0L : size;
    }

    private String dailyKey(LocalDate date) {
        return DAILY_KEY_PREFIX + date;
    }

    private long zSetSize(String key) {
        Long size = redisTemplate.opsForZSet().zCard(key);
        return size == null ? 0L : size;
    }

    private void registerGauge(MeterRegistry meterRegistry, String period, AtomicLong value) {
        Gauge.builder("fitpet.product.users.active", value, AtomicLong::get)
                .description("Unique authenticated active users")
                .tag("period", period)
                .register(meterRegistry);
    }
}
