package com.fitpet.server.user.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@DynamicUpdate
@Table(name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_oauth_provider_uid", columnNames = {"provider", "provider_uid"})
        },
        indexes = {
                @Index(name = "idx_users_nickname", columnList = "nick_name"),
                @Index(name = "idx_users_daily_step", columnList = "daily_step_count DESC, updated_at ASC"),
                @Index(name = "idx_users_gender_daily_step", columnList = "gender, daily_step_count DESC, updated_at ASC")
        })
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(length = 255)
    private String password;

    @Column(name = "nick_name", length = 60)
    private String nickname;

    private Integer age;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Column(name = "weight_kg")
    private Double weightKg;

    @Column(name = "target_weight_kg")
    private Double targetWeightKg;

    @Column(name = "height_cm")
    private Double heightCm;

    @Column
    private Double pbf;

    @Column(name = "target_pbf")
    private Double targetPbf;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Column(length = 50)
    private String provider;

    @Column(name = "provider_uid", length = 100)
    private String providerUid;

    @Column(name = "device_token", length = 255)
    private String deviceToken;

    @Column(name = "refresh_token", length = 255)
    private String refreshToken;

    @Default
    @Enumerated(EnumType.STRING)
    @Column(name = "registration_status", nullable = false, length = 20)
    private RegistrationStatus registrationStatus = RegistrationStatus.INCOMPLETE;

    @Column(name = "target_step_count")
    private Integer targetStepCount;

    @Default
    @Column(name = "daily_step_count")
    private Integer dailyStepCount = 0;

    @CreatedDate
    @Column(name = "created_at", updatable = false,
            columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at",
            columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    @Default
    @Column(name = "allow_activity_notification", nullable = false)
    private Boolean allowActivityNotification = true;

    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void update(
            String email,
            String nickname,
            Integer age,
            Gender gender,
            Double weightKg,
            Double targetWeightKg,
            Double heightCm,
            Double pbf,
            Double targetPbf,
            Integer targetStepCount
    ) {
        if (email != null && !email.isBlank()) {
            this.email = email;
        }
        if (nickname != null && !nickname.isBlank()) {
            this.nickname = nickname;
        }
        if (age != null) {
            this.age = age;
        }
        if (gender != null) {
            this.gender = gender;
        }
        if (weightKg != null) {
            this.weightKg = weightKg;
        }
        if (targetWeightKg != null) {
            this.targetWeightKg = targetWeightKg;
        }
        if (heightCm != null) {
            this.heightCm = heightCm;
        }
        if (pbf != null) {
            this.pbf = pbf;
        }
        if (targetPbf != null) {
            this.targetPbf = targetPbf;
        }
        if (targetStepCount != null) {
            this.targetStepCount = targetStepCount;
        }
    }

    public void updateDailyStepCount(int dailyStepCount) {
        if (dailyStepCount < 0) {
            throw new IllegalArgumentException("일일 걸음수는 음수일 수 없습니다.");
        }
        this.dailyStepCount = dailyStepCount;
    }

    public void userInformation(
            String nickname,
            int age,
            Gender gender,
            Double weightKg,
            Double heightCm,
            Double targetWeightKg,
            Double pbf,
            Double targetPbf,
            Integer targetStepCount
    ) {
        this.nickname = nickname;
        this.age = age;
        this.gender = gender;
        this.weightKg = weightKg;
        this.heightCm = heightCm;

        if (targetWeightKg == null) {
            this.targetWeightKg = null;
        } else {
            this.targetWeightKg = targetWeightKg;
        }
        if (pbf == null) {
            this.pbf = null;
        } else {
            this.pbf = pbf;
        }
        if (targetPbf == null) {
            this.targetPbf = null;
        } else {
            this.targetPbf = targetPbf;
        }
        if (targetStepCount == null) {
            this.targetStepCount = null;
        } else {
            this.targetStepCount = targetStepCount;
        }
        this.dailyStepCount = 0;
        this.registrationStatus = RegistrationStatus.COMPLETE;
    }

    public void linkSocial(String provider, String providerUid) {
        this.provider = provider;
        this.providerUid = providerUid;
    }

    public void updateLastAccessedAt() {
        this.lastAccessedAt = LocalDateTime.now();
    }

    public void changeActivityNotificationSetting(boolean allowed) {
        this.allowActivityNotification = allowed;
    }

    public void updateProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }
}