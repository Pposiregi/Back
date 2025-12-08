package com.fitpet.server.badge.presentation.dto;

import java.time.LocalDateTime;

public record BadgeCheckDto(
        Long badgeCheckId,
        Long userId,
        Long badgeId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
