package com.fitpet.server.mission.application.service;

import com.fitpet.server.mission.domain.entity.Mission;
import com.fitpet.server.mission.domain.entity.MissionCheck;
import com.fitpet.server.mission.domain.entity.MissionType;
import com.fitpet.server.mission.domain.repository.MissionCheckKey;
import com.fitpet.server.mission.domain.repository.MissionCheckRepository;
import com.fitpet.server.mission.domain.repository.MissionRepository;
import com.fitpet.server.user.domain.entity.User;
import com.fitpet.server.user.domain.repository.UserRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class MissionCheckBatchService {

    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final int USER_CHUNK_SIZE = 500;

    private final MissionRepository missionRepository;
    private final MissionCheckRepository missionCheckRepository;
    private final UserRepository userRepository;
    private final MissionCheckBatchSaveService missionCheckBatchSaveService;
    private final TransactionTemplate transactionTemplate;

    public int createDailyMissionChecks(LocalDate baseDate) {
        return createMissionChecks(MissionType.DAILY, baseDate);
    }

    public int createWeeklyMissionChecks(LocalDate baseDate) {
        return createMissionChecks(MissionType.WEEKLY, baseDate);
    }

    public int createMonthlyMissionChecks(LocalDate baseDate) {
        return createMissionChecks(MissionType.MONTHLY, baseDate);
    }

    public int createMissionChecks(MissionType type, LocalDate baseDate) {
        List<Mission> missions = missionRepository.findByType(type);
        if (missions.isEmpty()) {
            return 0;
        }

        List<Long> missionIds = missions.stream()
                .map(Mission::getId)
                .toList();
        PeriodRange period = resolvePeriod(type, baseDate);
        int created = 0;

        Slice<User> users = userRepository.findAll(PageRequest.of(0, USER_CHUNK_SIZE, Sort.by("id").ascending()));
        while (true) {
            if (users.isEmpty()) {
                break;
            }
            List<User> batchUsers = users.getContent();
            Integer batchCreated = transactionTemplate.execute(status -> createMissionChecksForUsers(
                    batchUsers,
                    missions,
                    missionIds,
                    type,
                    period
            ));
            if (batchCreated != null) {
                created += batchCreated;
            }
            if (!users.hasNext()) {
                break;
            }
            users = userRepository.findAll(users.nextPageable());
        }

        log.info("[MissionCheckBatchService] 미션 체크 생성 완료: type={}, periodStart={}, created={}",
                type, period.start(), created);
        return created;
    }

    private int createMissionChecksForUsers(
            List<User> users,
            List<Mission> missions,
            List<Long> missionIds,
            MissionType type,
            PeriodRange period
    ) {
        List<Long> userIds = users.stream()
                .map(User::getId)
                .toList();
        Set<MissionCheckKey> existingKeys = new HashSet<>(
                missionCheckRepository.findExistingKeys(userIds, missionIds, type, period.start())
        );

        List<MissionCheck> toSave = new java.util.ArrayList<>();
        for (User user : users) {
            for (Mission mission : missions) {
                if (existingKeys.contains(new MissionCheckKey(mission.getId(), user.getId()))) {
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

                toSave.add(missionCheck);
            }
        }

        if (toSave.isEmpty()) {
            return 0;
        }
        try {
            missionCheckRepository.saveAll(toSave);
            return toSave.size();
        } catch (DataIntegrityViolationException ex) {
            log.warn(
                    "[MissionCheckBatchService] 중복 생성 감지: type={}, periodStart={}, attempted={}",
                    type, period.start(), toSave.size()
            );
            int saved = 0;
            for (MissionCheck missionCheck : toSave) {
                try {
                    missionCheckBatchSaveService.saveInNewTx(missionCheck);
                    saved++;
                } catch (DataIntegrityViolationException ignore) {
                    // 중복이면 무시
                }
            }
            return saved;
        }
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
