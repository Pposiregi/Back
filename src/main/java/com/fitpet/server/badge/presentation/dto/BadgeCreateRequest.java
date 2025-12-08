package com.fitpet.server.badge.presentation.dto;

import com.fitpet.server.badge.domain.entity.BadgeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BadgeCreateRequest(
        @NotNull @NotBlank String title,
        @NotNull BadgeType type,
        Integer conditionDuration,
        Long conditionGoal,
        String description
) {
}
