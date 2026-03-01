package com.fitpet.server.mission.application.service;

import com.fitpet.server.meal.domain.entity.MealTime;
import com.fitpet.server.meal.domain.repository.MealRepository;
import com.fitpet.server.mission.application.dto.MissionCheckCommand;
import com.fitpet.server.mission.application.dto.MissionCheckResult;
import com.fitpet.server.mission.application.dto.MissionProgressResult;
import com.fitpet.server.mission.application.dto.MissionProgressUpdateItem;
import com.fitpet.server.mission.application.mapper.MissionCheckMapper;
import com.fitpet.server.mission.domain.entity.Mission;
import com.fitpet.server.mission.domain.entity.MissionCategory;
import com.fitpet.server.mission.domain.entity.MissionCheck;
import com.fitpet.server.mission.domain.entity.MissionType;
import com.fitpet.server.mission.domain.exception.MissionCheckAccessDeniedException;
import com.fitpet.server.mission.domain.exception.MissionCheckNotFoundException;
import com.fitpet.server.mission.domain.exception.MissionNotFoundException;
import com.fitpet.server.mission.domain.repository.MissionCheckRepository;
import com.fitpet.server.mission.domain.repository.MissionRepository;
import com.fitpet.server.pet.application.service.PetExpressionService;
import com.fitpet.server.pet.domain.entity.PetExpression;
import com.fitpet.server.user.domain.entity.User;
import com.fitpet.server.user.domain.exception.UserNotFoundException;
import com.fitpet.server.user.domain.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MissionCheckServiceImpl implements MissionCheckService {

    private final MissionRepository missionRepository;
    private final MissionCheckRepository missionCheckRepository;
    private final MissionCheckMapper missionCheckMapper;
    private final MealRepository mealRepository;
    private final UserRepository userRepository;
    private final PetExpressionService petExpressionService;
    private final MissionCompletionService missionCompletionService;

    @Override
    public MissionCheckResult upsertMissionCheck(Long missionId, Long userId, MissionCheckCommand request) {
        Mission mission = getMission(missionId);
        User user = getUser(userId);
        PeriodRange period = resolvePeriod(
                mission.getType(),
                request.actionDate()
        );

        UpdateResult result = upsertMissionCheck(mission, user, period, request.progressValue());
        MissionCheck saved = missionCheckRepository.save(result.missionCheck());

        log.info("[MissionCheckService] 수행 여부 저장: missionCheckId={}, missionId={}, userId={}",
                saved.getId(), missionId, userId);
        return missionCheckMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MissionCheckResult> getMissionChecks(Long userId) {
        List<MissionCheck> checks = missionCheckRepository.findRecentByUser(userId);
        return missionCheckMapper.toDtos(checks);
    }

    @Override
    public void deleteMissionCheck(Long userId, Long missionCheckId) {
        MissionCheck missionCheck = missionCheckRepository.findById(missionCheckId)
                .orElseThrow(MissionCheckNotFoundException::new);

        if (!missionCheck.getUser().getId().equals(userId)) {
            log.warn("[MissionCheckService] 삭제 권한 없음: 요청자 userId={}, 기록 소유자 userId={}, checkId={}",
                    userId, missionCheck.getUser().getId(), missionCheckId);
            throw new MissionCheckAccessDeniedException();
        }

        missionCheckRepository.delete(missionCheck);
        log.info("[MissionCheckService] 수행 여부 삭제: missionCheckId={}, userId={}", missionCheckId, userId);
    }

    @Override
    public MissionCheckResult completeMissionCheck(Long userId, Long missionCheckId) {
        MissionCheck completed = missionCompletionService.completeMission(userId, missionCheckId);
        petExpressionService.updateExpression(userId, PetExpression.HAPPY);
        log.info("[MissionCheckService] 수행 완료 처리: missionCheckId={}, userId={}", missionCheckId, userId);
        return missionCheckMapper.toDto(completed);
    }

    @Override
    public List<MissionProgressResult> getActiveMissions(Long userId, LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        List<MissionCheck> checks = missionCheckRepository.findActiveByUserAndDate(userId, targetDate);
        return checks.stream()
                .map(this::toProgressResult)
                .toList();
    }

    @Override
    public List<MissionProgressResult> getCompletedMissions(Long userId) {
        List<MissionCheck> checks = missionCheckRepository.findCompletedByUser(userId);
        return checks.stream()
                .map(this::toProgressResult)
                .toList();
    }

    @Override
    public List<MissionProgressUpdateItem> updateMealMissions(
            Long userId,
            LocalDate actionDate,
            MealTime mealTime
    ) {
        LocalDate date = actionDate != null ? actionDate : LocalDate.now();
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        boolean firstMealOfTime = mealTime != null
                && mealRepository.countByUserAndDayAndSequence(user, date, mealTime.getSequence()) == 1;

        return updateMealMissionsInternal(userId, date, mealTime, firstMealOfTime);
    }

    @Override
    public List<MissionProgressUpdateItem> updateStepMissions(
            Long userId,
            LocalDate actionDate,
            BigDecimal increment
    ) {
        LocalDate date = actionDate != null ? actionDate : LocalDate.now();
        BigDecimal delta = increment != null ? increment : BigDecimal.ZERO;
        if (delta.compareTo(BigDecimal.ZERO) <= 0) {
            return List.of();
        }

        List<MissionCheck> checks = findActiveChecks(userId, MissionCategory.STEP, date);

        if (checks.isEmpty()) {
            return List.of();
        }

        UpdateResult result = applyStepProgress(checks, delta);
        return result.updatedItems();
    }

    private static BigDecimal defaultProgress(BigDecimal progressValue) {
        return progressValue == null ? BigDecimal.ZERO : progressValue;
    }

    private static BigDecimal accumulateProgress(BigDecimal current, BigDecimal incoming) {
        BigDecimal safeCurrent = current == null ? BigDecimal.ZERO : current;
        BigDecimal safeIncoming = incoming == null ? BigDecimal.ZERO : incoming;
        return safeCurrent.add(safeIncoming);
    }

    private static boolean isCompleted(BigDecimal progress, BigDecimal goal) {
        if (progress == null || goal == null) {
            return false;
        }
        return progress.compareTo(goal) >= 0;
    }

    private static PeriodRange resolvePeriod(MissionType type, LocalDate baseDate) {
        LocalDate date = baseDate != null ? baseDate : LocalDate.now();
        return switch (type) {
            case DAILY -> new PeriodRange(date, date);
            case WEEKLY -> new PeriodRange(
                    resolveWeeklyStart(date),
                    date.with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY))
            );
            case MONTHLY -> new PeriodRange(
                    date.with(TemporalAdjusters.firstDayOfMonth()),
                    date.with(TemporalAdjusters.lastDayOfMonth())
            );
        };
    }

    private List<MissionProgressUpdateItem> updateMealMissionsInternal(
            Long userId,
            LocalDate date,
            MealTime mealTime,
            boolean firstMealOfTime
    ) {
        if (mealTime == null) {
            return List.of();
        }

        List<MissionCheck> checks = findActiveChecks(userId, MissionCategory.MEAL, date);

        if (checks.isEmpty()) {
            return List.of();
        }

        UpdateResult result = applyMealProgress(checks, mealTime, firstMealOfTime);
        return result.updatedItems();
    }

    private Mission getMission(Long missionId) {
        return missionRepository.findById(missionId)
                .orElseThrow(MissionNotFoundException::new);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
    }

    private UpdateResult upsertMissionCheck(
            Mission mission,
            User user,
            PeriodRange period,
            BigDecimal progressValue
    ) {
        var existing = missionCheckRepository.findByPeriodKey(
                mission.getId(),
                user.getId(),
                mission.getType(),
                period.start()
        );

        if (existing.isPresent()) {
            MissionCheck current = existing.get();
            applyProgressIfActive(current, progressValue);
            return new UpdateResult(current, List.of());
        }

        BigDecimal progress = defaultProgress(progressValue);
        MissionCheck created = MissionCheck.builder()
                .mission(mission)
                .user(user)
                .periodType(mission.getType())
                .periodStart(period.start())
                .periodEnd(period.end())
                .progressValue(progress)
                .completed(false)
                .completedAt(null)
                .build();

        return new UpdateResult(created, List.of());
    }

    private void applyProgressIfActive(MissionCheck current, BigDecimal delta) {
        if (current.isCompleted()) {
            return;
        }
        BigDecimal updatedProgress = accumulateProgress(current.getProgressValue(), delta);
        current.updateProgress(updatedProgress, false, null);
    }

    private UpdateResult applyStepProgress(List<MissionCheck> checks, BigDecimal delta) {
        List<MissionProgressUpdateItem> updated = new ArrayList<>();

        for (MissionCheck check : checks) {
            if (check.isCompleted()) {
                continue;
            }
            BigDecimal current = check.getProgressValue() == null ? BigDecimal.ZERO : check.getProgressValue();
            BigDecimal newProgress = current.add(delta);
            check.updateProgress(newProgress, false, null);
            missionCheckRepository.save(check);
            updated.add(toUpdateItem(check));
        }

        return new UpdateResult(null, updated);
    }

    private UpdateResult applyMealProgress(
            List<MissionCheck> checks,
            MealTime mealTime,
            boolean firstMealOfTime
    ) {
        List<MissionProgressUpdateItem> updated = new ArrayList<>();

        for (MissionCheck check : checks) {
            if (check.isCompleted()) {
                continue;
            }
            Mission mission = check.getMission();
            if (!shouldUpdateMealMission(mission, mealTime, firstMealOfTime)) {
                continue;
            }
            BigDecimal current = check.getProgressValue() == null ? BigDecimal.ZERO : check.getProgressValue();
            BigDecimal newProgress = current.add(BigDecimal.ONE);
            check.updateProgress(newProgress, false, null);
            missionCheckRepository.save(check);
            updated.add(toUpdateItem(check));
        }

        return new UpdateResult(null, updated);
    }

    private List<MissionCheck> findActiveChecks(Long userId, MissionCategory category, LocalDate date) {
        return missionCheckRepository.findActiveByUserAndCategoryAndDate(userId, category, date);
    }

    private static LocalDate resolveWeeklyStart(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
    }

    private static boolean shouldUpdateMealMission(
            Mission mission,
            MealTime mealTime,
            boolean firstMealOfTime
    ) {
        if (mealTime == null || !firstMealOfTime) {
            return false;
        }
        String title = mission.getTitle();
        switch (mealTime) {
            case BREAKFAST -> {
                if (matchesBreakfastTitle(title)) {
                    return true;
                }
            }
            case LUNCH -> {
                if (matchesLunchTitle(title)) {
                    return true;
                }
            }
            case DINNER -> {
                if (matchesDinnerTitle(title)) {
                    return true;
                }
            }
        }
        return isThreeMealTitle(title);
    }

    private static boolean matchesBreakfastTitle(String title) {
        return containsAny(title, "아침", "첫 끼", "첫끼");
    }

    private static boolean matchesLunchTitle(String title) {
        return containsAny(title, "점심", "균형");
    }

    private static boolean matchesDinnerTitle(String title) {
        return containsAny(title, "저녁", "마무리", "마지막");
    }

    private static boolean containsAny(String title, String... keywords) {
        if (title == null || title.isBlank()) {
            return false;
        }
        String lower = title.toLowerCase();
        for (String keyword : keywords) {
            if (lower.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isThreeMealTitle(String title) {
        if (title == null) {
            return false;
        }
        return title.contains("세 끼")
                || title.contains("세끼")
                || title.contains("3끼")
                || title.contains("3 끼");
    }

    private MissionProgressResult toProgressResult(MissionCheck check) {
        LocalDateTime periodStart = toStartOfDay(check.getPeriodStart());
        LocalDateTime periodEnd = toEndOfDay(check.getPeriodEnd());
        Mission mission = check.getMission();
        return new MissionProgressResult(
                check.getId(),
                mission.getId(),
                mission.getTitle(),
                mission.getCategory(),
                mission.getType(),
                periodStart,
                periodEnd,
                mission.getGoal(),
                check.getProgressValue(),
                check.isCompleted(),
                check.getCompletedAt()
        );
    }

    private MissionProgressUpdateItem toUpdateItem(MissionCheck check) {
        return new MissionProgressUpdateItem(
                check.getId(),
                check.getProgressValue(),
                check.isCompleted(),
                check.getCompletedAt()
        );
    }

    private static LocalDateTime toStartOfDay(LocalDate date) {
        return date != null ? date.atStartOfDay() : null;
    }

    private static LocalDateTime toEndOfDay(LocalDate date) {
        return date != null ? date.atTime(LocalTime.MAX) : null;
    }

    private record UpdateResult(
            MissionCheck missionCheck,
            List<MissionProgressUpdateItem> updatedItems
    ) {
    }

    private record PeriodRange(LocalDate start, LocalDate end) {
    }
}
