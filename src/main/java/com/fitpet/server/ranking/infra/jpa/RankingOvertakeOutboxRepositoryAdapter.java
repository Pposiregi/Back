package com.fitpet.server.ranking.infra.jpa;

import com.fitpet.server.ranking.domain.entity.RankingOvertakeOutbox;
import com.fitpet.server.ranking.domain.repository.RankingOvertakeOutboxRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RankingOvertakeOutboxRepositoryAdapter implements RankingOvertakeOutboxRepository {

    private final RankingOvertakeOutboxJpaRepository jpaRepository;

    @Override
    public RankingOvertakeOutbox save(RankingOvertakeOutbox outbox) {
        return jpaRepository.save(outbox);
    }

    @Override
    public List<RankingOvertakeOutbox> findPendingBatch(int limit) {
        return jpaRepository.lockPendingBatch(limit);
    }
}
