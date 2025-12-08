package com.fitpet.server.badge.presentation.dto;

import com.fitpet.server.badge.domain.entity.BadgeType;
import java.time.LocalDateTime;

public record BadgeDto(
        Long badgeId,
        String title,
        BadgeType type,
        Integer conditionDuration,
        Long conditionGoal,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
