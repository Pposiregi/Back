package com.fitpet.server.mission.presentation.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record MissionCheckRequest(
    @NotNull LocalDate actionDate,
    @NotNull BigDecimal progressValue
) {
}
