package com.fitpet.server.shared.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ProductMetricsCollector {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");
    private static final String PRODUCT_METRICS_QUERY = """
            SELECT
                (SELECT COUNT(*)
                   FROM users
                  WHERE created_at >= ?
                    AND created_at < ?
                    AND deleted_at IS NULL) AS new_users,
                (SELECT COUNT(DISTINCT m.user_id)
                   FROM meal m
                   JOIN users u ON u.user_id = m.user_id
                              AND u.deleted_at IS NULL
                  WHERE m.day = ?) AS meal_active_users,
                (SELECT COUNT(DISTINCT s.user_id)
                   FROM gps_session s
                   JOIN users u ON u.user_id = s.user_id
                              AND u.deleted_at IS NULL
                  WHERE s.end_time >= ?
                    AND s.end_time < ?
                    AND s.is_deleted = false) AS workout_completed_users,
                (SELECT COALESCE(AVG(CASE WHEN mc.is_completed = true THEN 1.0 ELSE 0.0 END), 0)
                   FROM mission_check mc
                   JOIN users u ON u.user_id = mc.user_id
                              AND u.deleted_at IS NULL
                  WHERE mc.period_start <= ?
                    AND mc.period_end >= ?) AS mission_completion_ratio
            """;

    private final JdbcTemplate jdbcTemplate;
    private final AtomicLong newUsers = new AtomicLong();
    private final AtomicLong mealActiveUsers = new AtomicLong();
    private final AtomicLong workoutCompletedUsers = new AtomicLong();
    private final AtomicReference<Double> missionCompletionRatio = new AtomicReference<>(0.0);

    public ProductMetricsCollector(JdbcTemplate jdbcTemplate, MeterRegistry meterRegistry) {
        this.jdbcTemplate = jdbcTemplate;
        registerGauge(meterRegistry, "fitpet.product.users.new.daily", newUsers);
        registerGauge(meterRegistry, "fitpet.product.meal.active.users.daily", mealActiveUsers);
        registerGauge(meterRegistry, "fitpet.product.workout.completed.users.daily", workoutCompletedUsers);
        Gauge.builder("fitpet.product.mission.completion.ratio", missionCompletionRatio, AtomicReference::get)
                .description("Completion ratio of mission checks active today")
                .register(meterRegistry);
    }

    @Scheduled(
            fixedDelayString = "${analytics.metrics.product-refresh-ms:300000}",
            initialDelayString = "${analytics.metrics.initial-delay-ms:10000}"
    )
    void refreshProductMetrics() {
        LocalDate today = LocalDate.now(SERVICE_ZONE);
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

        try {
            Map<String, Object> result = jdbcTemplate.queryForMap(
                    PRODUCT_METRICS_QUERY,
                    startOfDay,
                    endOfDay,
                    today,
                    startOfDay,
                    endOfDay,
                    today,
                    today
            );
            newUsers.set(longValue(result.get("new_users")));
            mealActiveUsers.set(longValue(result.get("meal_active_users")));
            workoutCompletedUsers.set(longValue(result.get("workout_completed_users")));
            missionCompletionRatio.set(doubleValue(result.get("mission_completion_ratio")));
        } catch (RuntimeException e) {
            log.warn("[ProductMetricsCollector] 제품 지표 갱신 실패", e);
        }
    }

    private long longValue(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private double doubleValue(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0.0;
    }

    private void registerGauge(MeterRegistry meterRegistry, String name, AtomicLong value) {
        Gauge.builder(name, value, AtomicLong::get)
                .description("FitPet product metric")
                .register(meterRegistry);
    }
}
