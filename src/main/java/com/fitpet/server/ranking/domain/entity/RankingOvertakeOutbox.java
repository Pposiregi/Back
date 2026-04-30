package com.fitpet.server.ranking.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 랭킹 추월 아웃박스 이벤트 테이블.
 *
 * <h2>아웃박스 패턴(Outbox Pattern) 적용 이유</h2>
 * <p>랭킹 업데이트는 Redis(Lua 스크립트)에서 일어나고, 알림 발송은 RabbitMQ를 통해
 * 별도 스레드에서 처리된다. 두 작업은 단일 트랜잭션으로 묶을 수 없으므로,
 * "MQ 발행 전 DB에 이벤트 기록 → 폴러가 PENDING 이벤트를 MQ에 발행 → PUBLISHED 처리"
 * 흐름으로 중간 유실을 방지한다.</p>
 *
 * <h2>보상 트랜잭션(Compensating Transaction) 고려</h2>
 * <p>알림은 비즈니스 크리티컬하지 않으므로 Redis 롤백 없이 best-effort 처리한다.
 * 아웃박스 저장 실패 시 알림이 누락되지만 랭킹 데이터 정합성은 유지된다.</p>
 */
@Entity
@Table(name = "ranking_overtake_outbox")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class RankingOvertakeOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "outbox_id")
    private Long id;

    /** 추월 당한 사용자 (알림 수신자) */
    @Column(name = "overtaken_user_id", nullable = false)
    private Long overtakenUserId;

    /** 추월 한 사용자 */
    @Column(name = "overtaking_user_id", nullable = false)
    private Long overtakingUserId;

    /** 랭킹 날짜 키 (예: 2026-04-15) */
    @Column(name = "date_key", nullable = false, length = 10)
    private String dateKey;

    /** 이 이벤트에서 피추월자를 추월한 사람 수 */
    @Column(name = "overtaker_count", nullable = false)
    @Builder.Default
    private int overtakerCount = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private OutboxStatus status = OutboxStatus.PENDING;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private int retryCount = 0;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    public void markClaimed() {
        this.status = OutboxStatus.CLAIMED;
    }

    public void markPending() {
        this.status = OutboxStatus.PENDING;
    }

    public void markPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
    }

    public void markFailed() {
        this.status = OutboxStatus.FAILED;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }
}
