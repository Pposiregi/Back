package com.fitpet.server.ranking.infra.jpa;

import com.fitpet.server.ranking.domain.entity.RankingOvertakeOutbox;
import com.fitpet.server.ranking.domain.repository.RankingOvertakeOutboxRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class RankingOvertakeOutboxRepositoryAdapter implements RankingOvertakeOutboxRepository {

    private final RankingOvertakeOutboxJpaRepository jpaRepository;

    @Override
    public RankingOvertakeOutbox save(RankingOvertakeOutbox outbox) {
        return jpaRepository.save(outbox);
    }

    @Override
    @Deprecated
    public List<RankingOvertakeOutbox> findPendingBatch(int limit) {
        return jpaRepository.lockPendingBatch(limit);
    }

    /**
     * PENDING 행을 CLAIMED 로 원자적으로 전환하고 반환한다.
     *
     * <p>FOR UPDATE SKIP LOCKED 로 행을 잠근 뒤 status=CLAIMED 로 커밋한다.
     * 이 트랜잭션이 커밋된 이후 MQ 발행을 수행해야
     * "발행 성공 + DB 커밋 실패 → 재발행" 경로를 차단할 수 있다.</p>
     */
    @Override
    @Transactional
    public List<RankingOvertakeOutbox> claimBatch(int limit) {
        List<RankingOvertakeOutbox> batch = jpaRepository.lockPendingBatch(limit);
        batch.forEach(RankingOvertakeOutbox::markClaimed);
        return jpaRepository.saveAll(batch);
    }
}
