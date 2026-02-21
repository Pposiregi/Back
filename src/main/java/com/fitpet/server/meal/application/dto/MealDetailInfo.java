package com.fitpet.server.meal.application.dto;

import java.time.LocalDate;
import lombok.Builder;

@Builder
public record MealDetailInfo(
        Long mealId,
        LocalDate day,
        String title,
        Integer kcal,
        Integer sequence,
        String imageUrl
) {
}