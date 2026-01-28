package com.fitpet.server.ranking.domain.repository;

import com.fitpet.server.ranking.domain.entity.Ranking;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RankingRepository extends JpaRepository<Ranking, Long> {

    // Write-Back 동기화용 (100명씩 Bulk 조회)
    List<Ranking> findAllByUserIdInAndDateKey(List<Long> userIds, String dateKey);

    // Redis 장애 시 Top 10 복구용 (Buffer 포함 20명)
    List<Ranking> findTop20ByDateKeyOrderByScoreDesc(String dateKey);

}