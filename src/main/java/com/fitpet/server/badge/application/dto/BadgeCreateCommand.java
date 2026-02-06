package com.fitpet.server.badge.application.dto;

import com.fitpet.server.badge.domain.entity.BadgeType;

public record BadgeCreateCommand(
        String title,
        BadgeType type,
        Integer conditionDuration,
        Long conditionGoal,
        String description,
        Long missionId
) {
}

