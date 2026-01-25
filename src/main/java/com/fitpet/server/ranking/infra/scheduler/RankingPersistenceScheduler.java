package com.fitpet.server.ranking.infra.scheduler;

import com.fitpet.server.ranking.application.service.RankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingPersistenceScheduler {

    private final RankingService rankingService;

    @Scheduled(fixedDelay = 60000) // 1분마다 실행
    public void persistRankingToDb() {
        rankingService.syncRedisToDatabase();
    }
}