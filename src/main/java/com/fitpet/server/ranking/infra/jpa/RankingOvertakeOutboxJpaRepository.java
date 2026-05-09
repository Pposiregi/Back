package com.fitpet.server.ranking.infra.jpa;

import com.fitpet.server.ranking.domain.entity.RankingOvertakeOutbox;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RankingOvertakeOutboxJpaRepository extends JpaRepository<RankingOvertakeOutbox, Long> {

    @Query(value = """
            SELECT *
            FROM ranking_overtake_outbox
            WHERE status = 'PENDING'
            ORDER BY created_at ASC
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<RankingOvertakeOutbox> lockPendingBatch(@Param("limit") int limit);
}
