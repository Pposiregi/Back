package com.fitpet.server.ranking.infra.scheduler;

import com.fitpet.server.ranking.application.service.RankingSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingPersistenceScheduler {

    private final RankingSyncService rankingSyncService;

    @Scheduled(fixedDelay = 60000) // 1분마다 실행
    public void persistRankingToDb() {
        try {
            log.info("[Scheduler] 랭킹 데이터 DB 동기화 시작");

            rankingSyncService.syncRedisToDatabase();

            log.info("[Scheduler] 랭킹 데이터 DB 동기화 완료");
        } catch (Exception e) {
            log.error("[Scheduler] 동기화 중 에러 발생", e);
        }
    }
}
