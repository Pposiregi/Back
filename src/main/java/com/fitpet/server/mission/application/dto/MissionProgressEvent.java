package com.fitpet.server.mission.application.dto;

import com.fitpet.server.mission.domain.entity.MissionCategory;
import com.fitpet.server.mission.domain.entity.MissionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MissionProgressEvent(
        String eventId,
        MissionProgressEventType eventType,
        Long userId,
        Long missionCheckId,
        Long missionId,
        MissionCategory category,
        MissionType periodType,
        BigDecimal goalValue,
        BigDecimal progressValue,
        boolean completed,
        LocalDateTime completedAt,
        LocalDateTime occurredAt
) {
}