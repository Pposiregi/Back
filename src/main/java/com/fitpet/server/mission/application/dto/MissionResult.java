package com.fitpet.server.mission.application.dto;

import com.fitpet.server.mission.domain.entity.MissionCategory;
import com.fitpet.server.mission.domain.entity.MissionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MissionResult(
    Long missionId,
    String title,
    String content,
    String description,
    MissionType type,
    MissionCategory category,
    BigDecimal goal,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
