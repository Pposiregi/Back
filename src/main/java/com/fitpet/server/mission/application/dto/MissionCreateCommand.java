package com.fitpet.server.mission.application.dto;

import com.fitpet.server.mission.domain.entity.MissionCategory;
import com.fitpet.server.mission.domain.entity.MealMissionPolicy;
import com.fitpet.server.mission.domain.entity.MissionType;
import java.math.BigDecimal;

public record MissionCreateCommand(
    String title,
    String content,
    String description,
    MissionType type,
    MissionCategory category,
    MealMissionPolicy mealPolicy,
    BigDecimal goal
) {
}
