package com.fitpet.server.dailywalk.presentation.dto.request;

import com.fitpet.server.dailywalk.application.dto.DailyWalkStepUpdateCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyWalkStepUpdateRequest(
        @NotNull @PastOrPresent
        LocalDate date,

        @NotNull @PositiveOrZero
        Integer step,

        @NotNull @PositiveOrZero
        BigDecimal distanceKm,

        @NotNull @PositiveOrZero
        Integer burnCalories
) {
    public DailyWalkStepUpdateCommand toCommand() {
        return new DailyWalkStepUpdateCommand(date, step, distanceKm, burnCalories);
    }
}
