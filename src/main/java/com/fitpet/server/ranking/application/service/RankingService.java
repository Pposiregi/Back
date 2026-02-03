package com.fitpet.server.ranking.application.service;

import com.fitpet.server.ranking.application.dto.RankingDto;
import java.util.List;

public interface RankingService {

    void updateScore(Long userId, int steps);

    List<RankingDto> getTop10();

    RankingDto getMyRank(Long userId);
}