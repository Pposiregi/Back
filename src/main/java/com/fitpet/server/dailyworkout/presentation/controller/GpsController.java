package com.fitpet.server.dailyworkout.presentation.controller;

import com.fitpet.server.dailyworkout.application.service.GpsSessionService;
import com.fitpet.server.dailyworkout.presentation.dto.request.GpsLogRequest;
import com.fitpet.server.dailyworkout.presentation.dto.request.SessionEndRequest;
import com.fitpet.server.dailyworkout.presentation.dto.request.SessionStartRequest;
import com.fitpet.server.dailyworkout.presentation.dto.response.GpsLogResponse;
import com.fitpet.server.dailyworkout.presentation.dto.response.GpsSessionDetailResponse;
import com.fitpet.server.dailyworkout.presentation.dto.response.GpsSessionStartResponse;
import com.fitpet.server.dailyworkout.presentation.dto.response.GpsSessionSummaryResponse;
import com.fitpet.server.dailyworkout.presentation.dto.response.SessionEndResponse;
import com.fitpet.server.shared.annotation.AuthUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/gps")
@RequiredArgsConstructor
@Validated
public class GpsController {

    private final GpsSessionService gpsSessionService;

    // [변경 1] 201 Created 반환
    @PostMapping("/start")
    public ResponseEntity<GpsSessionStartResponse> startSession(
            @AuthUser Long userId,
            @Valid @RequestBody SessionStartRequest request
    ) {
        GpsSessionStartResponse response = gpsSessionService.startSession(userId, request);

        // 생성된 리소스의 위치를 헤더에 알려주는 것이 정석 (Location Header)
        return ResponseEntity
                .created(URI.create("/gps/sessions/" + response.getSessionId()))
                .body(response);
    }

    // [변경 2] @AuthUser 추가 (보안 강화) 및 201 반환
    @PostMapping("/log")
    public ResponseEntity<GpsLogResponse> logGps(
            @AuthUser Long userId, // 내 세션에만 로그를 남길 수 있어야 함
            @Valid @RequestBody GpsLogRequest request
    ) {
        // Service 메서드 시그니처도 userId를 받도록 수정 필요
        GpsLogResponse response = gpsSessionService.logGps(userId, request);
        return ResponseEntity.created(URI.create("")).body(response);
    }

    // [변경 3] @AuthUser 추가 (보안 강화)
    @PostMapping("/end")
    public ResponseEntity<SessionEndResponse> endSession(
            @AuthUser Long userId, // 내 세션만 종료할 수 있어야 함
            @Valid @RequestBody SessionEndRequest request
    ) {
        // Service 메서드 시그니처도 userId를 받도록 수정 필요
        SessionEndResponse response = gpsSessionService.endSession(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<GpsSessionSummaryResponse>> getMonthlySessions(
            @AuthUser Long userId,
            @RequestParam @Min(2025) @Max(2100) int year,
            @RequestParam @Min(1) @Max(12) int month
    ) {
        List<GpsSessionSummaryResponse> sessions = gpsSessionService.getMonthlySessions(userId, year, month);
        // Tip: 실무에서는 List를 바로 리턴하기보다 Result<T> 같은 래퍼 클래스를 사용하는 것을 권장
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<GpsSessionDetailResponse> getSessionDetail(
            @AuthUser Long userId,
            @PathVariable Long sessionId
    ) {
        GpsSessionDetailResponse response = gpsSessionService.getSessionDetail(userId, sessionId);
        return ResponseEntity.ok(response);
    }
}