package com.fitpet.server.meal.application.dto;

import lombok.Builder;

@Builder
public record MealResult(
        Long mealId,
        String imageUrl,
        String uploadUrl
) {
}