package com.fitpet.server.dailywalk.application.service;

import com.fitpet.server.dailywalk.application.mapper.DailyWalkMapper;
import com.fitpet.server.dailywalk.domain.entity.DailyWalk;
import com.fitpet.server.dailywalk.domain.exception.DailyWalkNotFoundException;
import com.fitpet.server.dailywalk.domain.repository.DailyWalkRepository;
import com.fitpet.server.dailywalk.presentation.dto.request.DailyWalkCreateRequest;
import com.fitpet.server.dailywalk.presentation.dto.request.DailyWalkStepUpdateRequest;
import com.fitpet.server.dailywalk.presentation.dto.response.DailyStepSummaryResponse;
import com.fitpet.server.dailywalk.presentation.dto.response.DailyWalkResponse;
import com.fitpet.server.mission.application.service.MissionCheckService;
import com.fitpet.server.pet.application.service.PetExpressionService;
import com.fitpet.server.pet.domain.entity.PetExpression;
import com.fitpet.server.ranking.application.service.RankingService;
import com.fitpet.server.shared.exception.BusinessException;
import com.fitpet.server.shared.exception.ErrorCode;
import com.fitpet.server.user.domain.entity.User;
import com.fitpet.server.user.domain.exception.UserNotFoundException;
import com.fitpet.server.user.domain.repository.UserRepository;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.math.BigDecimal;
import java.time.Duration;
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
    private static final Duration TTL = Duration.ofDays(3);

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

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        Optional<DailyWalk> dbResult = dailyWalkRepository
                .findByUser_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(userId, start, end);

        if (dbResult.isPresent()) {
            return DailyWalkResponse.from(dbResult.get());
        }

        // Redis fallback (아직 DB에 반영되지 않은 오늘 데이터)
        if (date.equals(LocalDate.now())) {
            DailyWalkResponse cached = readFromRedisCache(userId, date.toString());
            if (cached != null) {
                log.info("[DailyWalkService] Redis 캐시에서 조회 성공: userId={}", userId);
                return cached;
            }
        }

        throw new DailyWalkNotFoundException();
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

        // Redis에 기존 데이터가 있으면 write-behind 방식으로 누적
        Object cachedSteps = redisTemplate.opsForHash().get(STEPS_KEY + dateStr, userIdStr);
        if (cachedSteps != null) {
            return updateRedisAndReturn(userId, userIdStr, dateStr, req.step(), req.distanceKm(), req.burnCalories(),
                    user, date);
        }

        // Redis 없음 → DB 확인
        Optional<DailyWalk> existing = dailyWalkRepository
                .findByUser_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(userId, startOfDay, endOfDay);

        if (existing.isPresent()) {
            DailyWalk walk = existing.get();
            // DB 데이터를 Redis로 올리고 write-behind로 전환
            int newStep = walk.getStep() + req.step();
            BigDecimal newDistance = walk.getDistanceKm().add(req.distanceKm());
            int newCalories = walk.getBurnCalories() + req.burnCalories();

            // 기존 DB 값 기준으로 Redis 초기화 후 Lua로 원자적 업데이트
            populateRedisCache(userIdStr, dateStr, walk.getStep(), walk.getDistanceKm(), walk.getBurnCalories());
            long finalStep = rankingService.updateStepHashAndRankingScore(
                    userId, newStep, req.step(), req.distanceKm(), req.burnCalories(), date);

            if (date.equals(LocalDate.now())) {
                handleDailyStepUpdate(user, (int) finalStep, date, req.step());
            }

            log.info("[DailyWalkService] write-behind 전환 완료: id={}, userId={}", walk.getId(), userId);
            return new DailyWalkResponse(walk.getId(), (int) finalStep, newDistance, newCalories,
                    walk.getCreatedAt(), LocalDateTime.now());
        }

        // 오늘 첫 기록 → DB에 저장 (ID 확보), Redis에도 캐싱
        DailyWalk walk = dailyWalkMapper.toEntity(req, user, startOfDay);
        DailyWalk saved = dailyWalkRepository.save(walk);

        populateRedisCache(userIdStr, dateStr, saved.getStep(), saved.getDistanceKm(), saved.getBurnCalories());

        if (date.equals(LocalDate.now())) {
            handleDailyStepUpdate(user, saved.getStep(), date, req.step());
        }

        log.info("[DailyWalkService] 저장 완료: dailyWalkId={}, userId={}", saved.getId(), userId);
        return DailyWalkResponse.from(saved);
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

        // Redis에 없으면 DB에서 로드
        Object cachedSteps = redisTemplate.opsForHash().get(STEPS_KEY + dateStr, userIdStr);
        if (cachedSteps == null) {
            DailyWalk walk = dailyWalkRepository
                    .findByUser_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(userId, start, end)
                    .orElseThrow(DailyWalkNotFoundException::new);
            populateRedisCache(userIdStr, dateStr, walk.getStep(), walk.getDistanceKm(), walk.getBurnCalories());
        }

        int estimatedTotal = estimateCurrentSteps(userIdStr, dateStr) + req.step();
        long newSteps = rankingService.updateStepHashAndRankingScore(
                userId, estimatedTotal, req.step(), req.distanceKm(), req.burnCalories(), req.date());

        if (req.date().equals(LocalDate.now())) {
            handleDailyStepUpdate(user, (int) newSteps, req.date(), req.step());
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

    private DailyWalkResponse updateRedisAndReturn(Long userId, String userIdStr, String dateStr,
                                                    int stepDelta, BigDecimal distanceDelta, int caloriesDelta,
                                                    User user, LocalDate date) {
        // 걸음수 Hash + 랭킹 ZSet을 단일 Lua 스크립트로 원자적 업데이트
        int estimatedTotal = estimateCurrentSteps(userIdStr, dateStr) + stepDelta;
        long newSteps = rankingService.updateStepHashAndRankingScore(
                userId, estimatedTotal, stepDelta, distanceDelta, caloriesDelta, date);

        if (date.equals(LocalDate.now())) {
            handleDailyStepUpdate(user, (int) newSteps, date, stepDelta);
        }

        log.info("[DailyWalkService] write-behind 원자적 업데이트 완료: userId={}, steps={}", userId, newSteps);
        return new DailyWalkResponse(null, (int) newSteps, null, null, null, LocalDateTime.now());
    }

    private void populateRedisCache(String userIdStr, String dateStr,
                                    int steps, BigDecimal distance, int calories) {
        String stepsKey = STEPS_KEY + dateStr;
        String distKey = DISTANCE_KEY + dateStr;
        String calKey = CALORIES_KEY + dateStr;

        redisTemplate.opsForHash().put(stepsKey, userIdStr, String.valueOf(steps));
        redisTemplate.opsForHash().put(distKey, userIdStr, distance.toPlainString());
        redisTemplate.opsForHash().put(calKey, userIdStr, String.valueOf(calories));

        redisTemplate.expire(stepsKey, TTL);
        redisTemplate.expire(distKey, TTL);
        redisTemplate.expire(calKey, TTL);
    }

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
