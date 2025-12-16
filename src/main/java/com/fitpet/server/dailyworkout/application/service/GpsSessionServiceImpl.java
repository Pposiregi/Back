package com.fitpet.server.dailyworkout.application.service;

import com.fitpet.server.dailyworkout.application.mapper.GpsMapper;
import com.fitpet.server.dailyworkout.domain.entity.GpsLog;
import com.fitpet.server.dailyworkout.domain.entity.GpsSession;
import com.fitpet.server.dailyworkout.domain.repository.GpsLogRepository;
import com.fitpet.server.dailyworkout.domain.repository.GpsSessionRepository;
import com.fitpet.server.dailyworkout.presentation.dto.request.GpsLogRequest;
import com.fitpet.server.dailyworkout.presentation.dto.request.SessionEndRequest;
import com.fitpet.server.dailyworkout.presentation.dto.request.SessionStartRequest;
import com.fitpet.server.dailyworkout.presentation.dto.response.GpsLogResponse;
import com.fitpet.server.dailyworkout.presentation.dto.response.GpsSessionStartResponse;
import com.fitpet.server.dailyworkout.presentation.dto.response.GpsSessionSummaryResponse;
import com.fitpet.server.dailyworkout.presentation.dto.response.SessionEndResponse;
import com.fitpet.server.shared.exception.BusinessException;
import com.fitpet.server.shared.exception.ErrorCode;
import com.fitpet.server.user.domain.entity.User;
import com.fitpet.server.user.domain.repository.UserRepository;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GpsSessionServiceImpl implements GpsSessionService {

    private final GpsSessionRepository gpsSessionRepository;
    private final GpsLogRepository gpsLogRepository;
    private final UserRepository userRepository;
    private final GpsMapper gpsMapper;

    //TODO 인증된 사용자의 UserId를 가져오도록 수정
    @Override
    public GpsSessionStartResponse startSession(SessionStartRequest request) {
        log.info("GPS 세션 시작 요청: userId={}", request.getUserId());

        User user = userRepository.findById(request.getUserId())
            .orElseThrow(() -> {
                log.warn("사용자를 찾을 수 없음: userId={}", request.getUserId());
                return new BusinessException(ErrorCode.USER_NOT_FOUND);
            });

        GpsSession newSession = GpsSession.builder()
            .user(user)
            .startTime(request.getStartTime())
            .build();

        GpsSession savedSession = gpsSessionRepository.save(newSession);
        log.info("GPS 세션 생성 완료: sessionId={}", savedSession.getId());

        return gpsMapper.toSessionStartResponse(savedSession);
    }

    @Override
    public GpsLogResponse logGps(GpsLogRequest request) {
        log.debug("GPS 로그 기록 요청: sessionId={}", request.getSessionId());

        GpsSession session = gpsSessionRepository.findById(request.getSessionId())
            .orElseThrow(() -> {
                log.warn("세션을 찾을 수 없음: sessionId={}", request.getSessionId());
                return new BusinessException(ErrorCode.SESSION_NOT_FOUND);
            });

        GpsLog newLog = gpsMapper.toGpsLogEntity(request, session);
        GpsLog savedLog = gpsLogRepository.save(newLog);
        log.debug("GPS 로그 저장 완료: logId={}", savedLog.getId());

        return gpsMapper.toGpsLogResponse(savedLog);
    }

    @Override
    public SessionEndResponse endSession(SessionEndRequest request) {
        log.info("GPS 세션 종료 요청: sessionId={}", request.getSessionId());

        GpsSession session = gpsSessionRepository.findById(request.getSessionId())
            .orElseThrow(() -> {
                log.warn("세션을 찾을 수 없음: sessionId={}", request.getSessionId());
                return new BusinessException(ErrorCode.SESSION_NOT_FOUND);
            });

        gpsMapper.updateSessionFromEndRequest(request, session);
        session.setEndTime(request.getEndTime());
        log.info("GPS 세션 종료 완료: sessionId={}", session.getId());

        return gpsMapper.toSessionEndResponse(session);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GpsSessionSummaryResponse> getMonthlySessions(
        Long userId, int year, int month
    ) {
        LocalDate startDate;
        try {
            startDate = LocalDate.of(year, month, 1);
        } catch (DateTimeException e) {
            log.warn("잘못된 연월 요청: year={}, month={}", year, month);
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = startDate.plusMonths(1).atStartOfDay();

        User user = userRepository.findById(userId)
            .orElseThrow(() -> {
                log.warn("사용자를 찾을 수 없음: userId={}", userId);
                return new BusinessException(ErrorCode.USER_NOT_FOUND);
            });

        List<GpsSession> sessions = gpsSessionRepository.findMonthlySessions(user, start, end);

        return sessions.stream()
            .map(session -> GpsSessionSummaryResponse.of(
                session.getId(),
                session.getStartTime(),
                session.getEndTime(),
                session.getTotalDistance()
            ))
            .collect(Collectors.toList());
    }
}