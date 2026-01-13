package com.fitpet.server.mission.presentation.dto;

import com.fitpet.server.mission.domain.entity.MissionCategory;
import com.fitpet.server.mission.domain.entity.MissionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record MissionCreateRequest(
    @NotBlank String title,
    String content,
    String description,
    @NotNull MissionType type,
    @NotNull MissionCategory category,
    @NotNull BigDecimal goal
) {
}
