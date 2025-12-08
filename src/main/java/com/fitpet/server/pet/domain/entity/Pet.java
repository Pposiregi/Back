package com.fitpet.server.pet.domain.entity;

import com.fitpet.server.user.domain.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "pet",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_pet_owner", columnNames = "user_id")
        },
        indexes = {
                @Index(name = "idx_pet_owner", columnList = "user_id")
        }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pet_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_pet_user"))
    private User owner;

    @Column(nullable = false, length = 60)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "pet_type", nullable = false, length = 20)
    private PetType petType;

    @Column(length = 9, nullable = false)
    private String color;

    @Column(name = "exp", nullable = true)
    private Long exp; // 기본 0

    @Enumerated(EnumType.STRING)
    @Column(name = "expression", nullable = false, length = 20)
    @Default
    private PetExpression expression = PetExpression.NEUTRAL;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void update(String name, PetType petType, String color) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (petType != null) {
            this.petType = petType;
        }
        if (color != null && !color.isBlank()) {
            this.color = color;
        }
    }

    public void changeExpression(PetExpression newExpression) {
        if (newExpression != null) {
            this.expression = newExpression;
        }
    }

    public void addExp(long amount) {
        if (amount <= 0) {
            return;
        }
        if (this.exp == null) {
            this.exp = 0L;
        }
        this.exp += amount;
    }

    @PrePersist
    void prePersist() {
        if (this.exp == null) {
            this.exp = 0L;
        }
        if (this.expression == null) {
            this.expression = PetExpression.NEUTRAL;
        }
    }
}
