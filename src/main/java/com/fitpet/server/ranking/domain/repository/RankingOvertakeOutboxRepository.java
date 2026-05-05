package com.fitpet.server.ranking.domain.repository;

import com.fitpet.server.ranking.domain.entity.RankingOvertakeOutbox;
import java.util.List;

public interface RankingOvertakeOutboxRepository {

    RankingOvertakeOutbox save(RankingOvertakeOutbox outbox);

    @Deprecated
    List<RankingOvertakeOutbox> findPendingBatch(int limit);

    List<RankingOvertakeOutbox> claimBatch(int limit);
}
