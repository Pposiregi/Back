package com.fitpet.server.user.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@DynamicUpdate
@EntityListeners(AuditingEntityListener.class)
@Table(name = "user_profile_images",
        indexes = @Index(name = "idx_user_profile_images_user_id_created_at",
                columnList = "user_id, created_at DESC"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserProfileImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "image_key", nullable = false, length = 255)
    private String imageKey;

    @Column(name = "is_current", nullable = false)
    private boolean current;

    @CreatedDate
    @Column(name = "created_at", updatable = false,
            columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    public static UserProfileImage createCurrent(Long userId, String imageKey) {
        return UserProfileImage.builder()
                .userId(userId)
                .imageKey(imageKey)
                .current(true)
                .build();
    }

    public static UserProfileImage createHistory(Long userId, String imageKey) {
        return UserProfileImage.builder()
                .userId(userId)
                .imageKey(imageKey)
                .current(false)
                .build();
    }

    public void deactivate() {
        this.current = false;
    }

    public void activate() {
        this.current = true;
    }
}
