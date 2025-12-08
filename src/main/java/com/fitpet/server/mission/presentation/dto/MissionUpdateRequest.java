package com.fitpet.server.mission.presentation.dto;

import com.fitpet.server.mission.domain.entity.MissionType;
import java.math.BigDecimal;

public record MissionUpdateRequest(
    String title,
    String content,
    MissionType type,
    BigDecimal goal
) {
}
