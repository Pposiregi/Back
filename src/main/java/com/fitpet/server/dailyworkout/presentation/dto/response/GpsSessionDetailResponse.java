package com.fitpet.server.dailyworkout.presentation.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record GpsSessionDetailResponse(
    Long sessionId,
    LocalDateTime startTime,
    LocalDateTime endTime,
    BigDecimal totalDistance,
    BigDecimal avgSpeedKmh,
    Integer stepCount,
    Integer burnCalories,
    List<GpsRouteLogResponse> routeLogs
) {
    public static GpsSessionDetailResponse of(
        Long sessionId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        BigDecimal totalDistance,
        BigDecimal avgSpeedKmh,
        Integer stepCount,
        Integer burnCalories,
        List<GpsRouteLogResponse> routeLogs
    ) {
        return new GpsSessionDetailResponse(sessionId, startTime, endTime, totalDistance, avgSpeedKmh, stepCount, burnCalories, routeLogs);
    }
}
