package com.fitpet.server.dailywalk.presentation.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyWalkCreateRequest(
        @NotNull @PositiveOrZero Integer step,

        @NotNull
        @DecimalMin("0.000")                  // 음수 방지
        @Digits(integer = 4, fraction = 3)    // DECIMAL(7,3) => 정수부 4자리, 소수부 3자리
        BigDecimal distanceKm,

        @NotNull @PositiveOrZero Integer burnCalories,

        @PastOrPresent
        LocalDate date
) {
}
