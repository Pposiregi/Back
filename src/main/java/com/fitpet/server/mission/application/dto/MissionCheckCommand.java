package com.fitpet.server.mission.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MissionCheckCommand(
    LocalDate actionDate,
    BigDecimal progressValue
) {
}
