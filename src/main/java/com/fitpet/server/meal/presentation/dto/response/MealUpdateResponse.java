package com.fitpet.server.meal.presentation.dto.response;

import com.fitpet.server.meal.application.dto.MealResult;

public record MealUpdateResponse(
        String imageUrl,
        String uploadUrl
) {
    public static MealUpdateResponse from(MealResult result) {
        return new MealUpdateResponse(
                result.imageKey(),
                result.uploadUrl()
        );
    }
}