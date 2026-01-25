package com.fitpet.server.ranking.application.service;

import com.fitpet.server.ranking.presentation.dto.RankingResponse;
import java.util.List;

public interface RankingService {

    void updateScore(Long userId, int steps);

    List<RankingResponse> getTop10();

    RankingResponse getMyRank(Long userId);

    // 모든 Dirty 유저를 한 번에 DB로 동기화
    void syncAllDirtyRanksToDb();

}