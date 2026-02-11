package com.fitpet.server.bodyhistory.application.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fitpet.server.bodyhistory.application.mapper.BodyHistoryMapper;
import com.fitpet.server.bodyhistory.domain.entity.BodyHistory;
import com.fitpet.server.bodyhistory.domain.exception.BodyHistoryNotFoundException;
import com.fitpet.server.bodyhistory.domain.repository.BodyHistoryRepository;
import com.fitpet.server.bodyhistory.presentation.dto.request.BodyHistoryCreateRequest;
import com.fitpet.server.bodyhistory.presentation.dto.request.BodyHistoryUpdateRequest;
import com.fitpet.server.bodyhistory.presentation.dto.response.BodyHistoryResponse;
import com.fitpet.server.shared.exception.BusinessException;
import com.fitpet.server.shared.exception.ErrorCode;
import com.fitpet.server.user.domain.entity.User;
import com.fitpet.server.user.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BodyHistoryServiceImpl implements BodyHistoryService {
    private final BodyHistoryRepository bodyHistoryRepository;
    private final UserRepository userRepository;
    private final BodyHistoryMapper bodyHistoryMapper;

    @Override
    public BodyHistoryResponse createBodyHistory(Long userId, BodyHistoryCreateRequest req) {
        log.debug("[BodyHistoryService] 기록 생성 요청: userId={}",userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("[BodyHistoryService] 사용자 조회 실패: userId={}",userId);
                    return new BusinessException(ErrorCode.USER_NOT_FOUND);
                });
    
        BodyHistory bodyHistory = bodyHistoryMapper.toEntity(req, user);
        BodyHistory savedBodyHistory = bodyHistoryRepository.save(bodyHistory);

        log.info("[BodyHistoryService] 기록 생성 완료: historyId={}, userId={}",
                savedBodyHistory.getId(), userId);
    
        return bodyHistoryMapper.toResponse(savedBodyHistory);
    }

    @Override
    @Transactional(readOnly = true)
    public BodyHistoryResponse findBodyHistoryById(Long historyId) {
        log.debug("[BodyHistoryService] historyId로 기록 조회 요청: historyId={}", historyId);

        BodyHistory foundHistory = bodyHistoryRepository.findById(historyId)
                .orElseThrow(() -> {
                    log.warn("[BodyHistoryService] historyId로 기록 조회 실패: historyId={}", historyId);
                    return new BodyHistoryNotFoundException();
                });

        log.info("[BodyHistoryService] historyId로 기록 조회 완료: historyId={}", foundHistory.getId());
        return bodyHistoryMapper.toResponse(foundHistory);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BodyHistoryResponse> findAllBodyHistoriesByUserId(Long userId) {
        log.debug("[BodyHistoryService] 사용자의 전체 기록 조회 요청: userId={}", userId);

        List<BodyHistory> histories = bodyHistoryRepository.findAllByUserId(userId);

        log.info("[BodyHistoryService] 사용자의 전체 기록 조회 완료: userId={}, count={}", userId, histories.size());
        return histories.stream()
                .map(bodyHistoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BodyHistoryResponse> findMonthlyBodyHistories(Long userId, int year, int month) {
        log.debug("[BodyHistoryService] 사용자의 월별 기록 조회 요청: userId={}, year={}, month={}", userId, year, month);

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<BodyHistory> histories = bodyHistoryRepository.findAllByUserIdAndDate(userId, startDate, endDate);

        log.info("[BodyHistoryService] 사용자의 월별 기록 조회 완료: userId={}, year={}, month={}, count={}",
                userId, year, month, histories.size());
        return histories.stream()
                .map(bodyHistoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public BodyHistoryResponse updateBodyHistory(Long historyId, BodyHistoryUpdateRequest req) {
        log.debug("[BodyHistoryService] 기록 수정 요청: historyId={}", historyId);

        BodyHistory existingHistory = bodyHistoryRepository.findById(historyId)
                .orElseThrow(() -> {
                    log.warn("[BodyHistoryService] 기록 수정 실패 - 대상 없음: historyId={}", historyId);
                    return new BodyHistoryNotFoundException();
                });

        bodyHistoryMapper.updateBodyHistory(req, existingHistory);

        log.info("[BodyHistoryService] 기록 수정 완료: historyId={}", existingHistory.getId());
        return bodyHistoryMapper.toResponse(existingHistory);
    }

    @Override
    public void deleteBodyHistory(Long historyId) {
        log.debug("[BodyHistoryService] 기록 삭제 요청: historyId={}", historyId);

        if (!bodyHistoryRepository.existsById(historyId)) {
            log.warn("[BodyHistoryService] 기록 삭제 실패 - 대상 없음: historyId={}", historyId);
            throw new BodyHistoryNotFoundException();
        }

        bodyHistoryRepository.deleteById(historyId);
        log.info("[BodyHistoryService] 기록 삭제 완료: historyId={}", historyId);
    }
}