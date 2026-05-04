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

    @Override
    @Transactional
    public List<RankingOvertakeOutbox> claimBatch(int limit) {
        List<RankingOvertakeOutbox> batch = jpaRepository.lockPendingBatch(limit);
        batch.forEach(RankingOvertakeOutbox::markClaimed);
        return jpaRepository.saveAll(batch);
    }
}
