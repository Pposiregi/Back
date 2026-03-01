package com.fitpet.server.meal.presentation.dto.response;

import com.fitpet.server.meal.application.dto.MealResult;

public record MealCreateResponse(
        Long mealId,
        String imageKey,
        String uploadUrl
) {

    public static MealCreateResponse from(MealResult result) {
        return new MealCreateResponse(
                result.mealId(),
                result.imageKey(),
                result.uploadUrl()
        );
    }
}