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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "gps_session")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
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

    public boolean isOwnedBy(Long userId) {
        if (this.user == null || userId == null) {
            return false;
        }
        return this.user.getId().equals(userId);
    }

    public void addDistance(BigDecimal distance) {
        if (distance == null || distance.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        if (this.totalDistance == null) {
            this.totalDistance = BigDecimal.ZERO;
        }
        this.totalDistance = this.totalDistance.add(distance);
    }
}