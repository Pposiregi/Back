package com.fitpet.server.ranking.domain.repository;

import com.fitpet.server.ranking.domain.entity.Ranking;
import com.fitpet.server.user.domain.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RankingRepository extends JpaRepository<Ranking, Long> {

    // 내 랭킹 데이터 찾기
    Optional<Ranking> findByUserAndDateKey(User user, String dateKey);

    // 오늘 날짜 모든 랭킹 데이터 가져오기
    List<Ranking> findAllByDateKey(String dateKey);
}