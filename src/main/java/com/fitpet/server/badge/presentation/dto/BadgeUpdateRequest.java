package com.fitpet.server.badge.presentation.dto;

import com.fitpet.server.badge.domain.entity.BadgeType;

public record BadgeUpdateRequest(
    String title,
    BadgeType type,
    Integer conditionDuration,
    Long conditionGoal,
    String description
) {
}
