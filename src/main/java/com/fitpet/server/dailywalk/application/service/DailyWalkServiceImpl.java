package com.fitpet.server.dailywalk.application.service;

import com.fitpet.server.dailywalk.application.mapper.DailyWalkMapper;
import com.fitpet.server.dailywalk.domain.entity.DailyWalk;
import com.fitpet.server.dailywalk.domain.exception.DailyWalkNotFoundException;
import com.fitpet.server.dailywalk.domain.repository.DailyWalkRepository;
import com.fitpet.server.dailywalk.presentation.dto.request.DailyWalkCreateRequest;
import com.fitpet.server.dailywalk.presentation.dto.request.DailyWalkStepUpdateRequest;
import com.fitpet.server.dailywalk.presentation.dto.response.DailyStepSummaryResponse;
import com.fitpet.server.dailywalk.presentation.dto.response.DailyWalkResponse;
import com.fitpet.server.pet.application.service.PetExpressionService;
import com.fitpet.server.pet.domain.entity.PetExpression;
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

        DailyWalk found = dailyWalkRepository
                .findByUser_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(userId, start, end)
                .orElseThrow(DailyWalkNotFoundException::new);

        log.info("[DailyWalkService] 사용자의 해당 날짜 조회 성공 : id={}, userId={}, date={} ",
                found.getId(), userId, date);
        return DailyWalkResponse.from(found);
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
                    int step = (walk != null && walk.getStep() != null) ? walk.getStep() : 0;
                    return new DailyStepSummaryResponse(date, step);
                })
                .toList();
    }


    @Override
    public DailyWalkResponse createDailyWalk(DailyWalkCreateRequest req) {
        log.debug("[DailyWalkService] 생성 요청: userId={}, step={}, distanceKm={}, burnCalories={}, date={}",
                req.userId(), req.step(), req.distanceKm(), req.burnCalories(), req.date());

        User user = userRepository.findById(req.userId())
                .orElseThrow(() -> {
                    log.warn("[DailyWalkService] 생성 실패 - 사용자 없음: userId={}", req.userId());
                    return new BusinessException(ErrorCode.USER_NOT_FOUND);
                });

        LocalDate date = (req.date() != null) ? req.date() : LocalDate.now();
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        Optional<DailyWalk> existing =
                dailyWalkRepository.findByUser_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        req.userId(), startOfDay, endOfDay);

        if (existing.isPresent()) {
            DailyWalk walk = existing.get();

            walk.update(req.step(), req.distanceKm(), req.burnCalories());

            if (date.equals(LocalDate.now())) {
                handleDailyStepUpdate(user, req.step());
            }

            log.info("[DailyWalkService] 업데이트 완료: id={}, userId={}, createdAt={}",
                    walk.getId(), req.userId(), walk.getCreatedAt());
            return DailyWalkResponse.from(walk);
        }

        DailyWalk walk = dailyWalkMapper.toEntity(req, user, startOfDay);
        DailyWalk saved = dailyWalkRepository.save(walk);

        if (date.equals(LocalDate.now())) {
            handleDailyStepUpdate(user, req.step());
        }

        log.info("[DailyWalkService] 저장 완료: dailyWalkId={}, userId={}, createdAt={}",
                saved.getId(), user.getId(), saved.getCreatedAt());

        return DailyWalkResponse.from(saved);
    }

    @Override
    public void updateDailyWalkStep(
            @NotNull Long userId,
            DailyWalkStepUpdateRequest req) {

        log.debug("[DailyWalkService] 걸음수 수정 요청 userId={}, req={}", userId, req);

        LocalDateTime start = req.date().atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("[DailyWalkService] 걸음수 수정 실패 - 사용자 없음: userId={}", userId);
                    return new UserNotFoundException();
                });

        int updated = dailyWalkRepository.updateStepByUserIdAndDate(
                userId,
                start,
                end,
                req.step(),
                req.distanceKm(),
                req.burnCalories()
        );
        if (updated == 0) {
            log.warn("[DailyWalkService] 걸음수 수정 실패(대상 없음): userId={}, date={}", userId, req.date());
            throw new DailyWalkNotFoundException();
        }

        if (req.date().equals(LocalDate.now())) {
            handleDailyStepUpdate(user, req.step());
        }

        log.info("[DailyWalkService] 걸음수 수정 완료: userId={}, req={}", userId, req);
    }

    @Override
    public void deleteDailyWalk(@NotNull Long dailyWalkId) {
        log.debug("[DailyWalkService] 삭제 요청: dailyWalkId={}", dailyWalkId);
        if (!dailyWalkRepository.existsById(dailyWalkId)) {
            log.warn("[DailyWalkService] 삭제 실패(대상 없음): dailyWalkId={}", dailyWalkId);
            throw new DailyWalkNotFoundException();
        }

        dailyWalkRepository.deleteById(dailyWalkId);
        log.info("[DailyWalkService] 삭제 완료: dailyWalkId={}", dailyWalkId);
    }

    private void handleDailyStepUpdate(User user, int newStep) {
        user.updateDailyStepCount(newStep);
        Integer target = user.getTargetStepCount();
        if (target != null && newStep >= target) {
            petExpressionService.updateExpression(user.getId(), PetExpression.PROUD);
        }
    }
}
