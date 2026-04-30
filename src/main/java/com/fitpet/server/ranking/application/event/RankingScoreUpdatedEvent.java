package com.fitpet.server.ranking.application.event;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 랭킹 점수 업데이트 이벤트.
 *
 * <p>Lua 스크립트로 Redis ZSet이 갱신된 직후 발행된다.
 * {@link com.fitpet.server.ranking.application.service.RankingOvertakeDetectorService}가
 * 비동기로 수신하여 추월 알림 파이프라인을 시작한다.</p>
 *
 * @param userId      점수를 올린 사용자
 * @param previousRank 업데이트 전 0-indexed 순위 (처음 진입이면 null)
 * @param date        랭킹 날짜
 */
@Getter
@AllArgsConstructor
public class RankingScoreUpdatedEvent {
    private final Long userId;
    private final Long previousRank;
    private final LocalDate date;
}
