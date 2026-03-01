package com.fitpet.server.meal.presentation.dto.response;

import com.fitpet.server.meal.application.dto.MealDetailInfo;
import java.time.LocalDate;

public record MealDetailResponse(
        Long mealId,
        LocalDate day,
        String title,
        Integer kcal,
        Integer sequence,
        String imageUrl,
        boolean existImage
) {
    public static MealDetailResponse from(MealDetailInfo info) {
        return new MealDetailResponse(
                info.mealId(),
                info.day(),
                info.title(),
                info.kcal(),
                info.sequence(),
                info.imageUrl(),
                info.existImage()
        );
    }
}