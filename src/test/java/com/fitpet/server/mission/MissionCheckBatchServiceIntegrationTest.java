package com.fitpet.server.mission;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitpet.server.mission.application.service.MissionCheckBatchService;
import com.fitpet.server.mission.domain.entity.Mission;
import com.fitpet.server.mission.domain.entity.MissionCategory;
import com.fitpet.server.mission.domain.entity.MissionType;
import com.fitpet.server.mission.domain.repository.MissionCheckRepository;
import com.fitpet.server.mission.domain.repository.MissionRepository;
import com.fitpet.server.user.domain.entity.User;
import com.fitpet.server.user.infra.jpa.UserJpaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@ActiveProfiles("local")
@Disabled("로컬 DB 수동 검증용 테스트")
class MissionCheckBatchServiceIntegrationTest {

    @Autowired
    private MissionCheckBatchService missionCheckBatchService;

    @Autowired
    private MissionRepository missionRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private MissionCheckRepository missionCheckRepository;

    @Test
    @DisplayName("지정 날짜로 일간/주간/월간 미션 체크를 생성하고 중복 생성을 막는다")
    void createMissionChecksForAllPeriodsWithSpecificDate() {
        // given
        User user = userJpaRepository.save(User.builder()
            .email("batch-test-" + System.nanoTime() + "@fitpet.test")
            .nickname("batch-tester-" + System.nanoTime())
            .build());

        Mission dailyMission = createMission("배치 테스트 데일리 미션", MissionType.DAILY);
        Mission weeklyMission = createMission("배치 테스트 주간 미션", MissionType.WEEKLY);
        Mission monthlyMission = createMission("배치 테스트 월간 미션", MissionType.MONTHLY);

        // 해당 날짜로 mission_check 생성
        LocalDate targetDate = LocalDate.of(2026, 3, 2);
        LocalDate weeklyStart = targetDate.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        LocalDate monthlyStart = targetDate.with(TemporalAdjusters.firstDayOfMonth());

        // when
        int firstDailyCreated = missionCheckBatchService.createDailyMissionChecks(targetDate);
        int secondDailyCreated = missionCheckBatchService.createDailyMissionChecks(targetDate);

        int firstWeeklyCreated = missionCheckBatchService.createWeeklyMissionChecks(targetDate);
        int secondWeeklyCreated = missionCheckBatchService.createWeeklyMissionChecks(targetDate);

        int firstMonthlyCreated = missionCheckBatchService.createMonthlyMissionChecks(targetDate);
        int secondMonthlyCreated = missionCheckBatchService.createMonthlyMissionChecks(targetDate);

        // then
        assertThat(user.getId()).isNotNull();
        assertThat(dailyMission.getId()).isNotNull();
        assertThat(weeklyMission.getId()).isNotNull();
        assertThat(monthlyMission.getId()).isNotNull();

        assertThat(firstDailyCreated).isGreaterThanOrEqualTo(1);
        assertThat(secondDailyCreated).isEqualTo(0);
        assertThat(firstWeeklyCreated).isGreaterThanOrEqualTo(1);
        assertThat(secondWeeklyCreated).isEqualTo(0);
        assertThat(firstMonthlyCreated).isGreaterThanOrEqualTo(1);
        assertThat(secondMonthlyCreated).isEqualTo(0);

        assertThat(missionCheckRepository.findByPeriodKey(
            dailyMission.getId(),
            user.getId(),
            MissionType.DAILY,
            targetDate
        )).isPresent();

        assertThat(missionCheckRepository.findByPeriodKey(
            weeklyMission.getId(),
            user.getId(),
            MissionType.WEEKLY,
            weeklyStart
        )).isPresent();

        assertThat(missionCheckRepository.findByPeriodKey(
            monthlyMission.getId(),
            user.getId(),
            MissionType.MONTHLY,
            monthlyStart
        )).isPresent();
    }

    private Mission createMission(String title, MissionType type) {
        return missionRepository.save(Mission.builder()
            .title(title)
            .content("통합 테스트용")
            .description("지정 날짜 생성 검증")
            .type(type)
            .category(MissionCategory.STEP)
            .goal(BigDecimal.valueOf(3000))
            .build());
    }
}
