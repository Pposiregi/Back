package com.fitpet.server.ranking.domain.entity;

public enum OutboxStatus {
    /** MQ 발행 대기 중 */
    PENDING,
    /** 폴러가 발행을 시도 중 (중간 상태) – 발행 성공 시 PUBLISHED, 실패 시 PENDING으로 복원 */
    CLAIMED,
    /** MQ 발행 완료 (소비는 MQ가 보장) */
    PUBLISHED,
    /** 재시도 초과 – 수동 확인 필요 */
    FAILED
}
