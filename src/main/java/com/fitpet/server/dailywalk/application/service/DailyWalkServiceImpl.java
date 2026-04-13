package com.fitpet.server.dailywalk.application.service;

import com.fitpet.server.dailywalk.application.dto.DailyStepSummaryResult;
import com.fitpet.server.dailywalk.application.dto.DailyWalkCreateCommand;
import com.fitpet.server.dailywalk.application.dto.DailyWalkResult;
import com.fitpet.server.dailywalk.application.dto.DailyWalkStepUpdateCommand;
import com.fitpet.server.dailywalk.application.mapper.DailyWalkMapper;
import com.fitpet.server.dailywalk.domain.entity.DailyWalk;
import com.fitpet.server.dailywalk.domain.exception.DailyWalkNotFoundException;
import com.fitpet.server.dailywalk.domain.repository.DailyWalkRepository;
import com.fitpet.server.mission.application.service.MissionCheckService;
import com.fitpet.server.pet.application.service.PetExpressionService;
import com.fitpet.server.pet.domain.entity.PetExpression;
import com.fitpet.server.ranking.application.service.RankingService;
import com.fitpet.server.shared.exception.BusinessException;
import com.fitpet.server.shared.exception.ErrorCode;
import com.fitpet.server.user.domain.entity.User;
import com.fitpet.server.user.domain.exception.UserNotFoundException;
import com.fitpet.server.user.domain.repository.UserRepository;
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
    private static final String ID_KEY = "dailywalk:id:";
    private static final String CREATED_AT_KEY = "dailywalk:createdAt:";
    private static final Duration REDIS_TTL = Duration.ofDays(3);

    @Override
    @Transactional(readOnly = true)
    public List<DailyWalkResult> getAllByUserId(Long userId) {
        log.debug("[DailyWalkService] 전체 조회 요청: userId={}", userId);
        List<DailyWalk> result = dailyWalkRepository.findAllByUser_Id(userId);
        log.info("[DailyWalkService] 전체 조회 완료: userId={}, count={}", userId, result.size());
        return result.stream().map(dailyWalkMapper::toResult).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DailyWalkResult getDailyWalkByUserIdAndDate(Long userId, LocalDate date) {
        log.debug("[DailyWalkService] 사용자의 해당 날짜 조회 요청 : userId={}, date={} ", userId, date);

        if (date.equals(LocalDate.now())) {
            DailyWalkResult cached = readFromRedisCache(userId, date.toString());
            if (cached != null) {
                log.info("[DailyWalkService] Redis 캐시에서 조회 성공: userId={}", userId);
                return cached;
            }
        }

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        return dailyWalkRepository
                .findByUser_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(userId, start, end)
                .map(dailyWalkMapper::toResult)
                .orElseThrow(DailyWalkNotFoundException::new);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyStepSummaryResult> getWeeklySteps(Long userId) {
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
                        step = readStepFromRedis(userId, today.toString());
                    } else {
                        step = 0;
                    }
                    return new DailyStepSummaryResult(date, step);
                })
                .toList();
    }

    @Override
    public DailyWalkResult createDailyWalk(Long userId, DailyWalkCreateCommand cmd) {
        log.debug("[DailyWalkService] 생성 요청: userId={}, step={}, distanceKm={}, burnCalories={}, date={}",
                userId, cmd.step(), cmd.distanceKm(), cmd.burnCalories(), cmd.date());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("[DailyWalkService] 생성 실패 - 사용자 없음: userId={}", userId);
                    return new BusinessException(ErrorCode.USER_NOT_FOUND);
                });

        LocalDate date = (cmd.date() != null) ? cmd.date() : LocalDate.now();
        String dateStr = date.toString();
        String userIdStr = String.valueOf(userId);
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        int previousSteps = estimateCurrentSteps(userIdStr, dateStr);
        int delta = Math.max(0, cmd.step() - previousSteps);

        if (previousSteps == 0) {
            DailyWalk walk = dailyWalkRepository
                    .findByUser_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(userId, startOfDay, endOfDay)
                    .orElseGet(() -> {
                        DailyWalk newWalk = dailyWalkMapper.toEntity(cmd, user, startOfDay);
                        DailyWalk saved = dailyWalkRepository.save(newWalk);
                        log.info("[DailyWalkService] 최초 기록 DB 저장: userId={}", userId);
                        return saved;
                    });
            storeMetaInRedis(userIdStr, dateStr, walk.getId(), walk.getCreatedAt());
        }

        long newSteps = rankingService.updateStepHashAndRankingScore(
                userId, cmd.step(), cmd.distanceKm(), cmd.burnCalories(), date);

        if (date.equals(LocalDate.now())) {
            handleDailyStepUpdate(user, (int) newSteps, date, delta);
        }

        log.info("[DailyWalkService] Redis 업데이트 완료: userId={}, steps={}", userId, newSteps);

        Object storedId = redisTemplate.opsForHash().get(ID_KEY + dateStr, userIdStr);
        Object storedCreatedAt = redisTemplate.opsForHash().get(CREATED_AT_KEY + dateStr, userIdStr);
        Long id = storedId != null ? Long.parseLong((String) storedId) : null;
        LocalDateTime createdAt = storedCreatedAt != null ? LocalDateTime.parse((String) storedCreatedAt) : null;

        return new DailyWalkResult(id, (int) newSteps, cmd.distanceKm(), cmd.burnCalories(), createdAt, LocalDateTime.now());
    }

    @Override
    public void updateDailyWalkStep(Long userId, DailyWalkStepUpdateCommand cmd) {
        log.debug("[DailyWalkService] 걸음수 수정 요청 userId={}, cmd={}", userId, cmd);

        String dateStr = cmd.date().toString();
        String userIdStr = String.valueOf(userId);
        LocalDateTime start = cmd.date().atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("[DailyWalkService] 걸음수 수정 실패 - 사용자 없음: userId={}", userId);
                    return new UserNotFoundException();
                });

        dailyWalkRepository
                .findByUser_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(userId, start, end)
                .orElseThrow(DailyWalkNotFoundException::new);

        int previousSteps = estimateCurrentSteps(userIdStr, dateStr);
        int delta = Math.max(0, cmd.step() - previousSteps);

        long newSteps = rankingService.updateStepHashAndRankingScore(
                userId, cmd.step(), cmd.distanceKm(), cmd.burnCalories(), cmd.date());

        if (cmd.date().equals(LocalDate.now())) {
            handleDailyStepUpdate(user, (int) newSteps, cmd.date(), delta);
        }

        log.info("[DailyWalkService] 걸음수 수정 완료 (write-behind): userId={}", userId);
    }

    @Override
    public void deleteDailyWalk(Long userId, Long dailyWalkId) {
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

    private DailyWalkResult readFromRedisCache(Long userId, String dateStr) {
        String userIdStr = String.valueOf(userId);
        Object steps = redisTemplate.opsForHash().get(STEPS_KEY + dateStr, userIdStr);
        if (steps == null) {
            return null;
        }
        Object distance = redisTemplate.opsForHash().get(DISTANCE_KEY + dateStr, userIdStr);
        Object calories = redisTemplate.opsForHash().get(CALORIES_KEY + dateStr, userIdStr);
        Object storedId = redisTemplate.opsForHash().get(ID_KEY + dateStr, userIdStr);
        Object storedCreatedAt = redisTemplate.opsForHash().get(CREATED_AT_KEY + dateStr, userIdStr);

        return new DailyWalkResult(
                storedId != null ? Long.parseLong((String) storedId) : null,
                Integer.parseInt((String) steps),
                distance != null ? new BigDecimal((String) distance) : BigDecimal.ZERO,
                calories != null ? Integer.parseInt((String) calories) : 0,
                storedCreatedAt != null ? LocalDateTime.parse((String) storedCreatedAt) : null,
                LocalDateTime.now()
        );
    }

    private void storeMetaInRedis(String userIdStr, String dateStr, Long id, LocalDateTime createdAt) {
        redisTemplate.opsForHash().put(ID_KEY + dateStr, userIdStr, String.valueOf(id));
        redisTemplate.opsForHash().put(CREATED_AT_KEY + dateStr, userIdStr, createdAt.toString());
        redisTemplate.expire(ID_KEY + dateStr, REDIS_TTL);
        redisTemplate.expire(CREATED_AT_KEY + dateStr, REDIS_TTL);
    }

    private void handleDailyStepUpdate(User user, int newStep, LocalDate date, int delta) {
        Integer target = user.getTargetStepCount();
        if (target != null && newStep >= target) {
            petExpressionService.updateExpression(user.getId(), PetExpression.PROUD);
        }

        if (delta > 0 && date.equals(LocalDate.now())) {
            missionCheckService.updateStepMissions(user.getId(), date, BigDecimal.valueOf(delta));
        }
    }
}
