package com.fitpet.server.mission.presentation.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record MissionCheckRequest(
    @NotNull LocalDate checkDate,
    @NotNull Boolean completed
) {
}
