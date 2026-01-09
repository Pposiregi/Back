package com.fitpet.server.mission.application.dto;

import com.fitpet.server.mission.domain.entity.MissionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record MissionCheckResult(
    Long missionCheckId,
    Long missionId,
    Long userId,
    boolean completed,
    BigDecimal progressValue,
    MissionType periodType,
    LocalDate periodStart,
    LocalDate periodEnd,
    LocalDateTime completedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
