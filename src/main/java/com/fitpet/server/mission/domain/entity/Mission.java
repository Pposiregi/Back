package com.fitpet.server.mission.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@Table(name = "mission")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Mission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mission_id")
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 255)
    private String content;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MissionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MissionCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_policy", length = 20)
    private MealMissionPolicy mealPolicy;

    @Column(nullable = false)
    private BigDecimal goal;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void update(
            String title,
            String content,
            String description,
            MissionType type,
            MissionCategory category,
            MealMissionPolicy mealPolicy,
            BigDecimal goal
    ) {
        if (title != null && !title.isBlank()) {
            this.title = title;
        }
        if (content != null) {
            this.content = content;
        }
        if (description != null) {
            this.description = description;
        }
        if (type != null) {
            this.type = type;
        }
        MissionCategory targetCategory = category != null ? category : this.category;
        if (category != null) {
            this.category = category;
        }
        if (targetCategory == MissionCategory.MEAL) {
            if (mealPolicy != null) {
                this.mealPolicy = mealPolicy;
            }
        } else {
            this.mealPolicy = null;
        }
        if (goal != null) {
            this.goal = goal;
        }
    }
}
