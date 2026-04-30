package com.fitpet.server.ranking.domain.repository;

import com.fitpet.server.ranking.domain.entity.OutboxStatus;
import com.fitpet.server.ranking.domain.entity.RankingOvertakeOutbox;
import java.util.List;

public interface RankingOvertakeOutboxRepository {

    RankingOvertakeOutbox save(RankingOvertakeOutbox outbox);

    /**
     * PENDING 상태의 아웃박스 이벤트를 생성 순서대로 최대 {@code limit}건 조회한다.
     * 폴러가 배치 단위로 호출한다.
     */
    List<RankingOvertakeOutbox> findPendingBatch(int limit);
}
