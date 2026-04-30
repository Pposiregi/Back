package com.fitpet.server.ranking.infra.jpa;

import com.fitpet.server.ranking.domain.entity.OutboxStatus;
import com.fitpet.server.ranking.domain.entity.RankingOvertakeOutbox;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RankingOvertakeOutboxJpaRepository extends JpaRepository<RankingOvertakeOutbox, Long> {

    List<RankingOvertakeOutbox> findByStatusOrderByCreatedAtAsc(OutboxStatus status, Pageable pageable);
}
