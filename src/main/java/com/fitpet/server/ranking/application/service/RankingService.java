package com.fitpet.server.ranking.application.service;

import com.fitpet.server.ranking.application.dto.RankingDto;
import com.fitpet.server.ranking.domain.type.RankingFilter;
import java.util.List;

public interface RankingService {

    void updateScore(Long userId, int steps);
    
    List<RankingDto> getTop10(RankingFilter filter);

    RankingDto getMyRank(Long userId, RankingFilter filter);
}