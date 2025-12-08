package com.fitpet.server.mission.presentation.dto;

import com.fitpet.server.mission.domain.entity.MissionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record MissionCreateRequest(
    @NotBlank String title,
    String content,
    @NotNull MissionType type,
    @NotNull BigDecimal goal
) {
}
