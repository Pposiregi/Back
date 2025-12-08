package com.fitpet.server.mission.presentation.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MissionCheckDto(
        Long missionCheckId,
        Long missionId,
        Long userId,
        boolean completed,
        LocalDate checkDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
