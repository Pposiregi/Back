package com.fitpet.server.shared.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class ProductMetricsCollectorTest {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    private JdbcTemplate jdbcTemplate;
    private SimpleMeterRegistry meterRegistry;
    private ProductMetricsCollector productMetricsCollector;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:product-metrics;MODE=MySQL;NON_KEYWORDS=DAY;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        jdbcTemplate = new JdbcTemplate(dataSource);
        resetSchema();
        meterRegistry = new SimpleMeterRegistry();
        productMetricsCollector = new ProductMetricsCollector(jdbcTemplate, meterRegistry);
    }

    @Test
    void refreshesMetricsWithDistinctActiveUsersAndExcludesWithdrawnUsers() {
        LocalDate today = LocalDate.now(SERVICE_ZONE);
        LocalDateTime todayNoon = today.atTime(12, 0);

        insertUser(1L, todayNoon, null);
        insertUser(2L, today.minusDays(1).atTime(12, 0), null);
        insertUser(3L, todayNoon, todayNoon);

        insertMeal(1L, today);
        insertMeal(1L, today);
        insertMeal(3L, today);

        insertWorkout(1L, todayNoon, false);
        insertWorkout(2L, today.minusDays(1).atTime(12, 0), false);
        insertWorkout(3L, todayNoon, false);

        insertMissionCheck(1L, true, today, today);
        insertMissionCheck(2L, false, today, today);
        insertMissionCheck(3L, true, today, today);

        productMetricsCollector.refreshProductMetrics();

        assertGauge("fitpet.product.users.new.daily", 1.0);
        assertGauge("fitpet.product.meal.active.users.daily", 1.0);
        assertGauge("fitpet.product.workout.completed.users.daily", 1.0);
        assertGauge("fitpet.product.mission.completion.ratio", 0.5);
    }

    @Test
    void exposesProductMetricsWithDashboardNames() {
        PrometheusMeterRegistry prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        new ProductMetricsCollector(jdbcTemplate, prometheusRegistry);

        String scrape = prometheusRegistry.scrape();

        assertThat(scrape).contains("fitpet_product_users_new_daily");
        assertThat(scrape).contains("fitpet_product_meal_active_users_daily");
        assertThat(scrape).contains("fitpet_product_workout_completed_users_daily");
        assertThat(scrape).contains("fitpet_product_mission_completion_ratio");
    }

    private void resetSchema() {
        jdbcTemplate.execute("DROP ALL OBJECTS");
        jdbcTemplate.execute("""
                CREATE TABLE users (
                    user_id BIGINT PRIMARY KEY,
                    created_at TIMESTAMP NOT NULL,
                    deleted_at TIMESTAMP NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE meal (
                    user_id BIGINT NOT NULL,
                    day DATE NOT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE gps_session (
                    user_id BIGINT NOT NULL,
                    end_time TIMESTAMP NULL,
                    is_deleted BOOLEAN NOT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE mission_check (
                    user_id BIGINT NOT NULL,
                    is_completed BOOLEAN NOT NULL,
                    period_start DATE NOT NULL,
                    period_end DATE NOT NULL
                )
                """);
    }

    private void insertUser(Long userId, LocalDateTime createdAt, LocalDateTime deletedAt) {
        jdbcTemplate.update(
                "INSERT INTO users (user_id, created_at, deleted_at) VALUES (?, ?, ?)",
                userId,
                createdAt,
                deletedAt
        );
    }

    private void insertMeal(Long userId, LocalDate day) {
        jdbcTemplate.update("INSERT INTO meal (user_id, day) VALUES (?, ?)", userId, day);
    }

    private void insertWorkout(Long userId, LocalDateTime endTime, boolean deleted) {
        jdbcTemplate.update(
                "INSERT INTO gps_session (user_id, end_time, is_deleted) VALUES (?, ?, ?)",
                userId,
                endTime,
                deleted
        );
    }

    private void insertMissionCheck(Long userId, boolean completed, LocalDate periodStart, LocalDate periodEnd) {
        jdbcTemplate.update(
                """
                INSERT INTO mission_check (user_id, is_completed, period_start, period_end)
                VALUES (?, ?, ?, ?)
                """,
                userId,
                completed,
                periodStart,
                periodEnd
        );
    }

    private void assertGauge(String name, double expected) {
        assertThat(meterRegistry.get(name).gauge().value()).isEqualTo(expected);
    }
}
