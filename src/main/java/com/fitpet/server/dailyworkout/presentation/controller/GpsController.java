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

    @PostMapping("/start")
    public ResponseEntity<GpsSessionStartResponse> startSession(
        @AuthUser Long userId,
        @Valid @RequestBody SessionStartRequest request
    ) {
        GpsSessionStartResponse response = gpsSessionService.startSession(userId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/log")
    public ResponseEntity<GpsLogResponse> logGps(@Valid @RequestBody GpsLogRequest request) {
        GpsLogResponse response = gpsSessionService.logGps(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/end")
    public ResponseEntity<SessionEndResponse> endSession(@Valid @RequestBody SessionEndRequest request) {
        SessionEndResponse response = gpsSessionService.endSession(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<GpsSessionSummaryResponse>> getMonthlySessions(
        @AuthUser Long userId,
        @RequestParam @Min(2025) @Max(2100) int year,
        @RequestParam @Min(1) @Max(12) int month
    ) {
        List<GpsSessionSummaryResponse> sessions = gpsSessionService.getMonthlySessions(userId, year, month);
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