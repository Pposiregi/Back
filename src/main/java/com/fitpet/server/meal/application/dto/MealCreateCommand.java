package com.fitpet.server.meal.application.dto;

import java.time.LocalDate;
import lombok.Builder;

@Builder
public record MealCreateCommand(
        LocalDate day,
        String title,
        Integer kcal,
        Integer sequence
) {
}