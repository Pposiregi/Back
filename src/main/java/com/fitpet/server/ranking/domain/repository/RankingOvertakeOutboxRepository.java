package com.fitpet.server.ranking.domain.repository;

import com.fitpet.server.ranking.domain.entity.RankingOvertakeOutbox;
import java.util.List;

public interface RankingOvertakeOutboxRepository {

    RankingOvertakeOutbox save(RankingOvertakeOutbox outbox);

    /**
     * PENDING 상태의 아웃박스 이벤트를 생성 순서대로 최대 {@code limit}건 조회한다.
     * 폴러가 배치 단위로 호출한다.
     *
     * @deprecated 폴러는 {@link #claimBatch(int)} 를 사용한다. 이 메서드는 조회 전용.
     */
    @Deprecated
    List<RankingOvertakeOutbox> findPendingBatch(int limit);

    /**
     * PENDING 행을 CLAIMED로 원자적으로 전환하고 반환한다.
     *
     * <p>SELECT FOR UPDATE SKIP LOCKED 로 행을 잠근 뒤 status=CLAIMED 로 커밋하여
     * 다른 인스턴스나 다음 폴링 주기가 같은 행을 가져가지 않도록 보장한다.
     * MQ 발행은 이 메서드의 트랜잭션이 커밋된 이후에 수행해야 한다.</p>
     */
    List<RankingOvertakeOutbox> claimBatch(int limit);
}
