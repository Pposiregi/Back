package com.fitpet.server.badge.application.dto;

import java.time.LocalDateTime;

public record BadgeCheckResult(
        Long badgeCheckId,
        Long userId,
        Long badgeId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

