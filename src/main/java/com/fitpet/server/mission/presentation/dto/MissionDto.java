package com.fitpet.server.mission.presentation.dto;

import com.fitpet.server.mission.domain.entity.MissionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MissionDto(
        Long missionId,
        String title,
        String content,
        MissionType type,
        BigDecimal goal,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
