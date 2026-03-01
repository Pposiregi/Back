package com.fitpet.server.badge.application.dto;

import com.fitpet.server.badge.domain.entity.BadgeType;
import java.time.LocalDateTime;

public record BadgeResult(
        Long badgeId,
        String title,
        BadgeType type,
        Integer conditionDuration,
        Long conditionGoal,
        String description,
        Long missionId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

