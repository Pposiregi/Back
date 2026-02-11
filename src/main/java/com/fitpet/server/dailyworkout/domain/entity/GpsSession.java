package com.fitpet.server.dailyworkout.domain.entity;

import com.fitpet.server.user.domain.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "gps_session")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class GpsSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "total_distance", precision = 10, scale = 2)
    private BigDecimal totalDistance;

    @Column(name = "avg_speed", precision = 5, scale = 2)
    private BigDecimal avgSpeed;

    @Column(name = "step_count")
    private Integer stepCount;

    @Column(name = "burn_calories")
    private Integer burnCalories;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder.Default
    @OneToMany(mappedBy = "gpsSession", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GpsLog> gpsLogs = new ArrayList<>();
    
    private static final double METS_WALKING = 3.8;
    private static final double METS_JOGGING = 7.0;
    private static final double METS_RUNNING = 10.0;
    private static final double DEFAULT_WEIGHT = 70.0;

    // 소유권 확인 로직
    public boolean isOwnedBy(Long userId) {
        if (this.user == null || userId == null) {
            return false;
        }
        return this.user.getId().equals(userId);
    }

    // 거리 누적 로직
    public void addDistance(BigDecimal distance) {
        if (distance == null || distance.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        if (this.totalDistance == null) {
            this.totalDistance = BigDecimal.ZERO;
        }
        this.totalDistance = this.totalDistance.add(distance);
    }

    // 세션 종료 및 통계 자동 계산
    public void endSession(LocalDateTime endTime, Integer requestStepCount, Integer requestCalories) {
        this.endTime = endTime;
        this.stepCount = (requestStepCount != null) ? requestStepCount : 0;

        long durationSeconds = ChronoUnit.SECONDS.between(this.startTime, this.endTime);
        if (durationSeconds < 1) {
            durationSeconds = 1;
        }

        double durationHours = durationSeconds / 3600.0;
        double totalKm = (this.totalDistance != null ? this.totalDistance.doubleValue() : 0.0) / 1000.0;

        double avgSpeedVal = 0.0;
        if (durationHours > 0) {
            avgSpeedVal = totalKm / durationHours;
        }
        this.avgSpeed = BigDecimal.valueOf(avgSpeedVal).setScale(2, RoundingMode.HALF_UP);

        if (requestCalories != null && requestCalories > 0) {
            this.burnCalories = requestCalories;
        } else {
            Double userWeight = DEFAULT_WEIGHT;

            if (this.user != null && this.user.getWeightKg() != null) {
                userWeight = this.user.getWeightKg();
            }

            double mets;
            if (avgSpeedVal < 6.0) {
                mets = METS_WALKING;
            } else if (avgSpeedVal < 8.0) {
                mets = METS_JOGGING;
            } else {
                mets = METS_RUNNING;
            }

            this.burnCalories = (int) (mets * userWeight * durationHours);
        }
    }
}