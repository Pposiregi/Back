package com.fitpet.server.ranking.application.service;

import com.fitpet.server.ranking.presentation.dto.RankingResponse;
import java.util.List;

public interface RankingService {

    void updateScore(Long userId, double distance);

    List<RankingResponse> getTop10();
    
    RankingResponse getMyRank(Long userId);
}