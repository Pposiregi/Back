package com.fitpet.server.mission.application.dto;

import com.fitpet.server.mission.domain.entity.MissionCategory;
import com.fitpet.server.mission.domain.entity.MissionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MissionProgressResult(
    Long missionCheckId,
    Long missionId,
    String title,
    MissionCategory category,
    MissionType periodType,
    LocalDateTime periodStart,
    LocalDateTime periodEnd,
    BigDecimal goalValue,
    BigDecimal progressValue,
    boolean completed,
    LocalDateTime completedAt
) {
}
