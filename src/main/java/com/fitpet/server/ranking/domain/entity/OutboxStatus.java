package com.fitpet.server.ranking.domain.entity;

public enum OutboxStatus {
    /** MQ 발행 대기 중 */
    PENDING,
    /** MQ 발행 완료 (소비는 MQ가 보장) */
    PUBLISHED,
    /** 재시도 초과 – 수동 확인 필요 */
    FAILED
}
