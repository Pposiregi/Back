package com.fitpet.server.dailywalk.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyWalkCreateCommand(
        Integer step,
        BigDecimal distanceKm,
        Integer burnCalories,
        LocalDate date
) {
}
