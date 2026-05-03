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
import com.fitpet.server.dailyworkout.presentation.dto.response.GpsRouteLogResponse;
import com.fitpet.server.dailyworkout.presentation.dto.response.GpsSessionDetailResponse;
import com.fitpet.server.dailyworkout.presentation.dto.response.GpsSessionStartResponse;
import com.fitpet.server.dailyworkout.presentation.dto.response.GpsSessionSummaryResponse;
import com.fitpet.server.dailyworkout.presentation.dto.response.SessionEndResponse;
import com.fitpet.server.shared.exception.BusinessException;
import com.fitpet.server.shared.exception.ErrorCode;
import com.fitpet.server.shared.util.GeometryUtil;
import com.fitpet.server.user.domain.entity.User;
import com.fitpet.server.user.domain.repository.UserRepository;
import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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

    // GPS 필터링 상수
    private static final double MIN_DISTANCE_METER = 2.0;       // 2m 미만 이동은 노이즈로 간주하고 무시
    private static final double MAX_HUMAN_SPEED_KMH = 45.0;     // 시속 45km 이상은 차량/오류
    private static final double MAX_NOISE_SPEED_KMH = 150.0;    // 시속 150km 이상은 명백한 GPS 튐

    @Override
    public GpsSessionStartResponse startSession(Long userId, SessionStartRequest request) {
        log.info("GPS 세션 시작 요청: userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("사용자를 찾을 수 없음: userId={}", userId);
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
    public GpsLogResponse logGps(Long userId, GpsLogRequest request) {

        GpsSession session = gpsSessionRepository.findActiveById(request.getSessionId())
                .orElseThrow(() -> {
                    log.warn("세션을 찾을 수 없음: sessionId={}", request.getSessionId());
                    return new BusinessException(ErrorCode.SESSION_NOT_FOUND);
                });

        if (!session.isOwnedBy(userId)) {
            log.warn("권한 없는 세션 접근 시도: requester={}, owner={}, sessionId={}",
                    userId, session.getUser().getId(), session.getId());
            throw new BusinessException(ErrorCode.SESSION_ACCESS_DENIED);
        }

        GpsLog lastLog = gpsLogRepository.findTopByGpsSessionOrderByRecordedAtDesc(session);

        if (lastLog != null) {
            // 시간 차이 계산 (초 단위)
            long timeDeltaSeconds = ChronoUnit.SECONDS.between(lastLog.getRecordedAt(), request.getRecordedAt());

            if (timeDeltaSeconds <= 0) {
                return gpsMapper.toGpsLogResponse(lastLog);
            }

            // 거리 계산 - 미터 단위
            BigDecimal distance = GeometryUtil.calculateDistance(
                    lastLog.getLatitude(), lastLog.getLongitude(),
                    request.getLatitude(), request.getLongitude()
            );
            double distanceMeters = distance.doubleValue();

            // 필터링: 2m 미만 이동은 무시
            if (distanceMeters < MIN_DISTANCE_METER) {
                return gpsMapper.toGpsLogResponse(lastLog);
            }

            // 속도 계산 (km/h)
            double speedKmh = (distanceMeters / timeDeltaSeconds) * 3.6;

            // 필터링: 45km/h 이상 무시 (차량 및 GPS 튀어오름 방지)
            if (speedKmh > MAX_HUMAN_SPEED_KMH) {
                if (speedKmh > MAX_NOISE_SPEED_KMH) {
                    log.warn("GPS Noise Detected: speed={}km/h, dist={}m", speedKmh, distanceMeters);
                } else {
                    log.warn("Vehicle Detected: speed={}km/h. Ignore log.", speedKmh);
                }
                // 이상 데이터는 저장하지 않고 이전 로그 상태 반환
                return gpsMapper.toGpsLogResponse(lastLog);
            }

            // 정상 데이터 처리: 세션 엔티티에 거리 누적
            session.addDistance(distance);
        }

        // 로그 저장
        GpsLog newLog = gpsMapper.toGpsLogEntity(request, session);
        GpsLog savedLog = gpsLogRepository.save(newLog);

        return gpsMapper.toGpsLogResponse(savedLog);
    }

    @Override
    public SessionEndResponse endSession(Long userId, SessionEndRequest request) {
        log.info("GPS 세션 종료 요청: sessionId={}", request.getSessionId());

        GpsSession session = gpsSessionRepository.findActiveById(request.getSessionId())
                .orElseThrow(() -> {
                    log.warn("세션을 찾을 수 없음: sessionId={}", request.getSessionId());
                    return new BusinessException(ErrorCode.SESSION_NOT_FOUND);
                });

        if (!session.isOwnedBy(userId)) {
            log.warn("권한 없는 세션 종료 시도: requester={}, owner={}", userId, session.getUser().getId());
            throw new BusinessException(ErrorCode.SESSION_ACCESS_DENIED);
        }

        //  내부 로직으로 '소모 칼로리(체중 기반 METs)'와 '평균 속도'를 자동 계산
        session.endSession(
                request.getEndTime(),
                request.getStepCount(),
                request.getBurnCalories(),
                request.getTotalDistance()
        );

        log.info("GPS 세션 종료 완료: sessionId={}, totalDistance={}, burnCalories={}",
                session.getId(), session.getTotalDistance(), session.getBurnCalories());

        return gpsMapper.toSessionEndResponse(session);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GpsSessionSummaryResponse> getMonthlySessions(Long userId, int year, int month) {
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
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

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

    @Override
    @Transactional(readOnly = true)
    public GpsSessionDetailResponse getSessionDetail(Long userId, Long sessionId) {
        GpsSession session = gpsSessionRepository.findActiveById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));

        if (!session.isOwnedBy(userId)) {
            log.warn("세션 조회 권한 없음: userId={}, ownerId={}", userId, session.getUser().getId());
            throw new BusinessException(ErrorCode.SESSION_ACCESS_DENIED);
        }

        List<GpsRouteLogResponse> routeLogs = gpsLogRepository
                .findByGpsSessionOrderByRecordedAtAsc(session)
                .stream()
                .map(log -> GpsRouteLogResponse.of(log.getLatitude(), log.getLongitude(), log.getAltitude()))
                .collect(Collectors.toList());

        return GpsSessionDetailResponse.of(
                session.getId(),
                session.getStartTime(),
                session.getEndTime(),
                session.getTotalDistance(),
                session.getAvgSpeed(),
                session.getStepCount(),
                session.getBurnCalories(),
                routeLogs
        );
    }

    @Override
    public void deleteSession(Long userId, Long sessionId) {
        GpsSession session = gpsSessionRepository.findActiveById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));

        if (!session.isOwnedBy(userId)) {
            log.warn("세션 삭제 권한 없음: userId={}, ownerId={}, sessionId={}",
                    userId, session.getUser().getId(), session.getId());
            throw new BusinessException(ErrorCode.SESSION_ACCESS_DENIED);
        }

        session.delete();
        log.info("GPS 세션 삭제 완료: userId={}, sessionId={}", userId, sessionId);
    }
}
