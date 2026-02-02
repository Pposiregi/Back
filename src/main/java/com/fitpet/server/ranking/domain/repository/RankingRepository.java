package com.fitpet.server.ranking.domain.repository;

import com.fitpet.server.ranking.domain.entity.Ranking;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RankingRepository extends JpaRepository<Ranking, Long> {

    // Write-Back 동기화용 (100명씩 Bulk 조회)
    List<Ranking> findAllByUserIdInAndDateKey(List<Long> userIds, String dateKey);

    @Query("SELECT r FROM Ranking r WHERE r.dateKey = :dateKey ORDER BY r.score DESC, r.updatedAt ASC")
    List<Ranking> findTopRankings(@Param("dateKey") String dateKey, Pageable pageable);

}