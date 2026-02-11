package com.fitpet.server.meal.application.dto;

import lombok.Builder;

@Builder
public record MealUpdateCommand(
        String title,
        Integer kcal,
        Integer sequence,
        Boolean changeImage
) {
}