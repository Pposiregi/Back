package com.fitpet.server.dailyworkout.presentation.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GpsSessionSummaryResponse(
    Long sessionId,
    LocalDateTime startTime,
    LocalDateTime endTime,
    BigDecimal totalDistance
) {
    public static GpsSessionSummaryResponse of(
        Long sessionId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        BigDecimal totalDistance
    ) {
        return new GpsSessionSummaryResponse(sessionId, startTime, endTime, totalDistance);
    }
}
