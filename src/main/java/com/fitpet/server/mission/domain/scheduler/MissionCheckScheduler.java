package com.fitpet.server.mission.domain.scheduler;

import com.fitpet.server.mission.application.service.MissionCheckBatchService;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MissionCheckScheduler {

    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");

    private final MissionCheckBatchService missionCheckBatchService;

    @Scheduled(cron = "${mission.scheduler.daily-cron:0 0 0 * * *}", zone = "Asia/Seoul")
    public void createDailyMissionChecks() {
        LocalDate today = LocalDate.now(ZONE_ID);
        int created = missionCheckBatchService.createDailyMissionChecks(today);
        log.info("[MissionCheckScheduler] 데일리 미션 생성 완료: date={}, created={}", today, created);
    }

    @Scheduled(cron = "${mission.scheduler.weekly-cron:0 0 0 * * MON}", zone = "Asia/Seoul")
    public void createWeeklyMissionChecks() {
        LocalDate today = LocalDate.now(ZONE_ID);
        int created = missionCheckBatchService.createWeeklyMissionChecks(today);
        log.info("[MissionCheckScheduler] 주간 미션 생성 완료: date={}, created={}", today, created);
    }

    @Scheduled(cron = "${mission.scheduler.monthly-cron:0 0 0 1 * *}", zone = "Asia/Seoul")
    public void createMonthlyMissionChecks() {
        LocalDate today = LocalDate.now(ZONE_ID);
        int created = missionCheckBatchService.createMonthlyMissionChecks(today);
        log.info("[MissionCheckScheduler] 월간 미션 생성 완료: date={}, created={}", today, created);
    }
}
