package com.fitpet.server.ranking.application.service;

import com.fitpet.server.ranking.application.event.RankingScoreUpdatedEvent;

/**
 * 랭킹 추월 감지 서비스.
 *
 * <p>Redis ZSet 에서 순위 변동을 계산하여 추월 당한 사용자들을 찾고
 * 아웃박스 이벤트를 저장한다.</p>
 */
public interface RankingOvertakeDetectorService {

    /**
     * {@link RankingScoreUpdatedEvent} 수신 후 비동기로 추월 여부를 감지하고
     * 아웃박스 이벤트를 저장한다.
     *
     * <p>{@code @Async}로 실행되므로 호출 스레드를 블록하지 않는다.</p>
     */
    void onRankingScoreUpdated(RankingScoreUpdatedEvent event);
}
