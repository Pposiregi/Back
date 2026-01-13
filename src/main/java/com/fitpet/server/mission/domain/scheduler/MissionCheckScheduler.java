package com.fitpet.server.mission.domain.scheduler;

import com.fitpet.server.mission.domain.entity.Mission;
import com.fitpet.server.mission.domain.entity.MissionCheck;
import com.fitpet.server.mission.domain.entity.MissionType;
import com.fitpet.server.mission.domain.repository.MissionCheckRepository;
import com.fitpet.server.mission.domain.repository.MissionRepository;
import com.fitpet.server.user.domain.entity.User;
import com.fitpet.server.user.domain.repository.UserRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class MissionCheckScheduler {

    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");

    private final MissionRepository missionRepository;
    private final MissionCheckRepository missionCheckRepository;
    private final UserRepository userRepository;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    @Transactional
    public void createDailyMissionChecks() {
        LocalDate today = LocalDate.now(ZONE_ID);
        int created = createMissionChecks(MissionType.DAILY, today);
        log.info("[MissionCheckScheduler] 데일리 미션 생성 완료: date={}, created={}", today, created);
    }

    @Scheduled(cron = "0 0 0 * * MON", zone = "Asia/Seoul")
    @Transactional
    public void createWeeklyMissionChecks() {
        LocalDate today = LocalDate.now(ZONE_ID);
        int created = createMissionChecks(MissionType.WEEKLY, today);
        log.info("[MissionCheckScheduler] 주간 미션 생성 완료: date={}, created={}", today, created);
    }

    @Scheduled(cron = "0 0 0 1 * *", zone = "Asia/Seoul")
    @Transactional
    public void createMonthlyMissionChecks() {
        LocalDate today = LocalDate.now(ZONE_ID);
        int created = createMissionChecks(MissionType.MONTHLY, today);
        log.info("[MissionCheckScheduler] 월간 미션 생성 완료: date={}, created={}", today, created);
    }

    private int createMissionChecks(MissionType type, LocalDate baseDate) {
        List<Mission> missions = missionRepository.findByType(type);
        if (missions.isEmpty()) {
            return 0;
        }

        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            return 0;
        }

        PeriodRange period = resolvePeriod(type, baseDate);
        int created = 0;

        for (User user : users) {
            for (Mission mission : missions) {
                boolean exists = missionCheckRepository.findByPeriodKey(
                        mission.getId(),
                        user.getId(),
                        mission.getType(),
                        period.start()
                ).isPresent();
                if (exists) {
                    continue;
                }

                MissionCheck missionCheck = MissionCheck.builder()
                        .mission(mission)
                        .user(user)
                        .periodType(type)
                        .periodStart(period.start())
                        .periodEnd(period.end())
                        .progressValue(BigDecimal.ZERO)
                        .completed(false)
                        .completedAt(null)
                        .build();

                missionCheckRepository.save(missionCheck);
                created++;
            }
        }

        return created;
    }

    private static PeriodRange resolvePeriod(MissionType type, LocalDate baseDate) {
        LocalDate date = baseDate != null ? baseDate : LocalDate.now(ZONE_ID);
        return switch (type) {
            case DAILY -> new PeriodRange(date, date);
            case WEEKLY -> new PeriodRange(resolveWeeklyStart(date), resolveWeeklyEnd(date));
            case MONTHLY -> new PeriodRange(
                    date.with(TemporalAdjusters.firstDayOfMonth()),
                    date.with(TemporalAdjusters.lastDayOfMonth())
            );
        };
    }

    private static LocalDate resolveWeeklyStart(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private static LocalDate resolveWeeklyEnd(LocalDate date) {
        return date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
    }

    private record PeriodRange(LocalDate start, LocalDate end) {
    }
}
