package com.fitpet.server.badge.presentation.dto;

import jakarta.validation.constraints.NotNull;

public record BadgeCheckCreateRequest(
        @NotNull Long badgeId
) {
}
