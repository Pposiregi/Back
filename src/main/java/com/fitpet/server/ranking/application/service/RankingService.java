package com.fitpet.server.ranking.application.service;

import com.fitpet.server.ranking.application.dto.RankingDto;
import com.fitpet.server.ranking.domain.type.RankingFilter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface RankingService {

    void updateScore(Long userId, int steps);

    /**
     * 걸음수 Hash(write-behind)와 랭킹 ZSet을 단일 Lua 스크립트로 원자적 업데이트
     * @return 업데이트 후 누적 걸음수
     */
    long updateStepHashAndRankingScore(Long userId, int totalSteps, int stepDelta,
                                       BigDecimal distanceDelta, int caloriesDelta,
                                       LocalDate date);

    List<RankingDto> getTop10(RankingFilter filter);

    RankingDto getMyRank(Long userId, RankingFilter filter);
}