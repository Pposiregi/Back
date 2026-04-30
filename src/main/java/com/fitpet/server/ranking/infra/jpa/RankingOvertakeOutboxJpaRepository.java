package com.fitpet.server.ranking.infra.jpa;

import com.fitpet.server.ranking.domain.entity.RankingOvertakeOutbox;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RankingOvertakeOutboxJpaRepository extends JpaRepository<RankingOvertakeOutbox, Long> {

    /**
     * PENDING 상태 행을 최대 {@code limit}건 비관적 락으로 조회한다.
     *
     * <p>FOR UPDATE SKIP LOCKED 덕분에 다른 인스턴스가 이미 처리 중인 행은
     * 건너뛰어 멀티 인스턴스 환경에서 중복 발행을 방지한다.
     * 호출부에 반드시 {@code @Transactional}이 존재해야 트랜잭션 종료 시까지 락이 유지된다.</p>
     */
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
