package com.fitpet.server.mission.presentation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MissionProgressUpdateItem(
    Long missionCheckId,
    BigDecimal progressValue,
    boolean completed,
    LocalDateTime completedAt
) {
}
