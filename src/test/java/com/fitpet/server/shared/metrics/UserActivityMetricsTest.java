package com.fitpet.server.shared.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

@ExtendWith(MockitoExtension.class)
class UserActivityMetricsTest {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    private SimpleMeterRegistry meterRegistry;
    private UserActivityMetrics userActivityMetrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        userActivityMetrics = new UserActivityMetrics(redisTemplate, meterRegistry);
    }

    @Test
    void recordsUserInDailySetAndRollingMonthlyIndex() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(setOperations.add(anyString(), eq("7"))).thenReturn(1L);

        userActivityMetrics.recordActiveUser(7L);

        LocalDate today = LocalDate.now(SERVICE_ZONE);
        String dailyKey = "analytics:active-users:daily:" + today;
        verify(setOperations).add(dailyKey, "7");
        verify(redisTemplate).expire(dailyKey, Duration.ofDays(40));
        verify(zSetOperations).add(eq("analytics:active-users:last-seen"), eq("7"), anyDouble());
    }

    @Test
    void refreshesActiveUserGaugesFromRedisSetSizes() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        LocalDate today = LocalDate.now(SERVICE_ZONE);
        String dailyKey = "analytics:active-users:daily:" + today;
        when(setOperations.size(dailyKey)).thenReturn(12L);
        when(zSetOperations.zCard("analytics:active-users:last-seen")).thenReturn(45L);

        userActivityMetrics.refreshActiveUserGauges();

        verify(zSetOperations).removeRangeByScore(
                eq("analytics:active-users:last-seen"),
                eq(0.0),
                anyDouble()
        );
        assertThat(meterRegistry.get("fitpet.product.users.active")
                .tag("period", "daily")
                .gauge()
                .value()).isEqualTo(12.0);
        assertThat(meterRegistry.get("fitpet.product.users.active")
                .tag("period", "monthly")
                .gauge()
                .value()).isEqualTo(45.0);
    }

    @Test
    void exposesActiveUserMetricsWithDashboardNames() {
        PrometheusMeterRegistry prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        new UserActivityMetrics(redisTemplate, prometheusRegistry);

        String scrape = prometheusRegistry.scrape();

        assertThat(scrape).contains("fitpet_product_users_active{period=\"daily\"}");
        assertThat(scrape).contains("fitpet_product_users_active{period=\"monthly\"}");
    }
}
