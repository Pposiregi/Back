package com.fitpet.server.dailywalk.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DailyWalkResult(
        Long id,
        Integer step,
        BigDecimal distanceKm,
        Integer burnCalories,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
