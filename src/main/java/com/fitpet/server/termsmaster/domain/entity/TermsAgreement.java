package com.fitpet.server.termsmaster.domain.entity;

import com.fitpet.server.user.domain.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(
    name = "user_terms_agreement",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_terms_agreement_user_terms",
        columnNames = {"user_id", "terms_id"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class TermsAgreement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "agreement_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "terms_id", nullable = false)
    private Terms terms;

    @Column(nullable = false)
    private boolean isAgreed;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public TermsAgreement(User user, Terms terms, boolean isAgreed) {
        this.user = user;
        this.terms = terms;
        this.isAgreed = isAgreed;
    }

    public void updateAgreed(boolean isAgreed) {
        this.isAgreed = isAgreed;
    }
}