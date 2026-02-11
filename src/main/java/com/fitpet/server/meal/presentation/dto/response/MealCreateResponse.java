package com.fitpet.server.meal.presentation.dto.response;

import com.fitpet.server.meal.application.dto.MealResult;

public record MealCreateResponse(
        Long mealId,
        String imageUrl,
        String uploadUrl
) {

    public static MealCreateResponse from(MealResult result) {
        return new MealCreateResponse(
                result.mealId(),
                result.imageUrl(),
                result.uploadUrl()
        );
    }
}