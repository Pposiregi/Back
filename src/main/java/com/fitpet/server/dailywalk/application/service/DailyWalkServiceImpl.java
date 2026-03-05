package com.fitpet.server.dailywalk.application.service;

import com.fitpet.server.dailywalk.application.mapper.DailyWalkMapper;
import com.fitpet.server.dailywalk.domain.entity.DailyWalk;
import com.fitpet.server.dailywalk.domain.exception.DailyWalkNotFoundException;
import com.fitpet.server.dailywalk.domain.repository.DailyWalkRepository;
import com.fitpet.server.dailywalk.presentation.dto.request.DailyWalkCreateRequest;
import com.fitpet.server.dailywalk.presentation.dto.request.DailyWalkStepUpdateRequest;
import com.fitpet.server.dailywalk.presentation.dto.response.DailyStepSummaryResponse;
import com.fitpet.server.dailywalk.presentation.dto.response.DailyWalkResponse;
import com.fitpet.server.ranking.application.service.RankingService;
import com.fitpet.server.shared.exception.BusinessException;
import com.fitpet.server.shared.exception.ErrorCode;
import com.fitpet.server.user.domain.entity.User;
import com.fitpet.server.user.domain.exception.UserNotFoundException;
import com.fitpet.server.user.domain.repository.UserRepository;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DailyWalkServiceImpl implements DailyWalkService {

    private final DailyWalkRepository dailyWalkRepository;
    private final UserRepository userRepository;
    private final DailyWalkMapper dailyWalkMapper;
    private final PetExpressionService petExpressionService;
    private final RankingService rankingService;
    private final MissionCheckService missionCheckService;
    private final StringRedisTemplate redisTemplate;

    private static final String STEPS_KEY = "dailywalk:steps:";
    private static final String DISTANCE_KEY = "dailywalk:distance:";
    private static final String CALORIES_KEY = "dailywalk:calories:";

    @Override
    @Transactional(readOnly = true)
    public List<DailyWalkResponse> getAllByUserId(@NotNull Long userId) {
        log.debug("[DailyWalkService] 전체 조회 요청: userId={}", userId);
        List<DailyWalk> result = dailyWalkRepository.findAllByUser_Id(userId);
        log.info("[DailyWalkService] 전체 조회 완료: userId={}, count={}", userId, result.size());
        return result.stream().map(DailyWalkResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DailyWalkResponse getDailyWalkByUserIdAndDate(@NotNull Long userId,
                                                         @NotNull @PastOrPresent LocalDate date) {
        log.debug("[DailyWalkService] 사용자의 해당 날짜 조회 요청 : userId={}, date={} ", userId, date);

        // 오늘 데이터는 Redis가 항상 최신 → Redis 먼저 확인
        if (date.equals(LocalDate.now())) {
            DailyWalkResponse cached = readFromRedisCache(userId, date.toString());
            if (cached != null) {
                log.info("[DailyWalkService] Redis 캐시에서 조회 성공: userId={}", userId);
                return cached;
            }
        }

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        return dailyWalkRepository
                .findByUser_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(userId, start, end)
                .map(DailyWalkResponse::from)
                .orElseThrow(DailyWalkNotFoundException::new);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyStepSummaryResponse> getWeeklySteps(@NotNull Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(6);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        if (userRepository.findById(userId).isEmpty()) {
            log.warn("[DailyWalkService] 주간 걸음수 조회 실패 - 사용자 없음: userId={}", userId);
            throw new UserNotFoundException();
        }

        List<DailyWalk> walks = dailyWalkRepository
                .findAllByUser_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(userId, start, end);
        Map<LocalDate, DailyWalk> byDate = new HashMap<>();
        for (DailyWalk walk : walks) {
            LocalDate walkDate = walk.getCreatedAt().toLocalDate();
            byDate.put(walkDate, walk);
        }

        return startDate.datesUntil(today.plusDays(1))
                .map(date -> {
                    DailyWalk walk = byDate.get(date);
                    int step;
                    if (walk != null) {
                        step = walk.getStep() != null ? walk.getStep() : 0;
                    } else if (date.equals(today)) {
                        // 오늘 데이터가 DB에 없으면 Redis에서 확인
                        step = readStepFromRedis(userId, today.toString());
                    } else {
                        step = 0;
                    }
                    return new DailyStepSummaryResponse(date, step);
                })
                .toList();
    }

    @Override
    public DailyWalkResponse createDailyWalk(Long userId, DailyWalkCreateRequest req) {
        log.debug("[DailyWalkService] 생성 요청: userId={}, step={}, distanceKm={}, burnCalories={}, date={}",
                userId, req.step(), req.distanceKm(), req.burnCalories(), req.date());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("[DailyWalkService] 생성 실패 - 사용자 없음: userId={}", userId);
                    return new BusinessException(ErrorCode.USER_NOT_FOUND);
                });

        LocalDate date = (req.date() != null) ? req.date() : LocalDate.now();
        String dateStr = date.toString();
        String userIdStr = String.valueOf(userId);
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        // 미션 업데이트용 delta 계산 (이전 Redis 값과 비교)
        int previousSteps = estimateCurrentSteps(userIdStr, dateStr);
        int delta = Math.max(0, req.step() - previousSteps);

        // Redis에 데이터가 없을 때만 DB 확인 (첫 기록 여부)
        // Redis가 있으면 이미 DB 레코드가 존재함 → DB 조회 스킵
        if (previousSteps == 0) {
            Optional<DailyWalk> existing = dailyWalkRepository
                    .findByUser_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(userId, startOfDay, endOfDay);
            if (existing.isEmpty()) {
                DailyWalk walk = dailyWalkMapper.toEntity(req, user, startOfDay);
                dailyWalkRepository.save(walk);
                log.info("[DailyWalkService] 최초 기록 DB 저장: userId={}", userId);
            }
        }

        // Redis에 최신 총합 SET (프론트가 항상 최신 총합을 전송)
        long newSteps = rankingService.updateStepHashAndRankingScore(
                userId, req.step(), req.distanceKm(), req.burnCalories(), date);

        if (date.equals(LocalDate.now())) {
            handleDailyStepUpdate(user, (int) newSteps, date, delta);
        }

        log.info("[DailyWalkService] Redis 업데이트 완료: userId={}, steps={}", userId, newSteps);
        return new DailyWalkResponse(null, (int) newSteps, req.distanceKm(), req.burnCalories(), null, LocalDateTime.now());
    }

    @Override
    public void updateDailyWalkStep(@NotNull Long userId, DailyWalkStepUpdateRequest req) {
        log.debug("[DailyWalkService] 걸음수 수정 요청 userId={}, req={}", userId, req);

        String dateStr = req.date().toString();
        String userIdStr = String.valueOf(userId);
        LocalDateTime start = req.date().atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("[DailyWalkService] 걸음수 수정 실패 - 사용자 없음: userId={}", userId);
                    return new UserNotFoundException();
                });

        // DB 레코드 존재 여부 확인
        dailyWalkRepository
                .findByUser_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(userId, start, end)
                .orElseThrow(DailyWalkNotFoundException::new);

        // 미션 업데이트용 delta 계산
        int previousSteps = estimateCurrentSteps(userIdStr, dateStr);
        int delta = Math.max(0, req.step() - previousSteps);

        // Redis에 최신 총합 SET
        long newSteps = rankingService.updateStepHashAndRankingScore(
                userId, req.step(), req.distanceKm(), req.burnCalories(), req.date());

        if (req.date().equals(LocalDate.now())) {
            handleDailyStepUpdate(user, (int) newSteps, req.date(), delta);
        }

        log.info("[DailyWalkService] 걸음수 수정 완료 (write-behind): userId={}", userId);
    }

    @Override
    public void deleteDailyWalk(@NotNull Long userId, @NotNull Long dailyWalkId) {
        log.debug("[DailyWalkService] 삭제 요청: userId={}, dailyWalkId={}", userId, dailyWalkId);

        DailyWalk dailyWalk = dailyWalkRepository.findById(dailyWalkId)
                .orElseThrow(DailyWalkNotFoundException::new);

        if (!dailyWalk.getUser().getId().equals(userId)) {
            log.warn("[DailyWalkService] 삭제 실패(권한 없음): userId={}, ownerId={}, dailyWalkId={}",
                    userId, dailyWalk.getUser().getId(), dailyWalkId);
            //TODO: 예외수정하기
            throw new RuntimeException("본인의 산책 기록만 삭제할 수 있습니다.");
        }

        dailyWalkRepository.delete(dailyWalk);
        log.info("[DailyWalkService] 삭제 완료: dailyWalkId={}", dailyWalkId);
    }

    // ─── private helpers ───────────────────────────────────────────────────────

    private int estimateCurrentSteps(String userIdStr, String dateStr) {
        Object val = redisTemplate.opsForHash().get(STEPS_KEY + dateStr, userIdStr);
        return val != null ? Integer.parseInt((String) val) : 0;
    }

    private int readStepFromRedis(Long userId, String dateStr) {
        Object val = redisTemplate.opsForHash().get(STEPS_KEY + dateStr, String.valueOf(userId));
        return val != null ? Integer.parseInt((String) val) : 0;
    }

    private DailyWalkResponse readFromRedisCache(Long userId, String dateStr) {
        String userIdStr = String.valueOf(userId);
        Object steps = redisTemplate.opsForHash().get(STEPS_KEY + dateStr, userIdStr);
        if (steps == null) {
            return null;
        }
        Object distance = redisTemplate.opsForHash().get(DISTANCE_KEY + dateStr, userIdStr);
        Object calories = redisTemplate.opsForHash().get(CALORIES_KEY + dateStr, userIdStr);

        return new DailyWalkResponse(
                null,
                Integer.parseInt((String) steps),
                distance != null ? new BigDecimal((String) distance) : BigDecimal.ZERO,
                calories != null ? Integer.parseInt((String) calories) : 0,
                null,
                LocalDateTime.now()
        );
    }

    private void handleDailyStepUpdate(User user, int newStep, LocalDate date, int delta) {
        user.updateDailyStepCount(newStep);

        Integer target = user.getTargetStepCount();
        if (target != null && newStep >= target) {
            petExpressionService.updateExpression(user.getId(), PetExpression.PROUD);
        }

        if (delta > 0 && date.equals(LocalDate.now())) {
            missionCheckService.updateStepMissions(user.getId(), date, BigDecimal.valueOf(delta));
        }
        // 랭킹 ZSet 업데이트는 Lua 스크립트에서 이미 처리됨
    }
}
