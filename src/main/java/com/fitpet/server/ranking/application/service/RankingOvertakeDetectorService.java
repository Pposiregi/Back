package com.fitpet.server.ranking.application.service;

import com.fitpet.server.ranking.application.event.RankingScoreUpdatedEvent;

public interface RankingOvertakeDetectorService {

    void onRankingScoreUpdated(RankingScoreUpdatedEvent event);
}
