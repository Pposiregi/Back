package com.fitpet.server.bodyhistory.presentation.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;

public record BodyHistoryCreateRequest(
        @NotNull(message = "키는 필수입니다.")
        @PositiveOrZero(message = "키는 0 이상이어야 합니다.")
        BigDecimal heightCm,

        @NotNull(message = "몸무게는 필수입니다.")
        @PositiveOrZero(message = "몸무게는 0 이상이어야 합니다.")
        BigDecimal weightKg,

        @NotNull(message = "체지방률은 필수입니다.")
        @DecimalMin(value = "0.0", message = "체지방률은 0 이상이어야 합니다.")
        @DecimalMax(value = "100.0", message = "체지방률은 100 이하이어야 합니다.")
        BigDecimal pbf,

        @NotNull(message = "측정일은 필수입니다.")
        @PastOrPresent(message = "측정일은 오늘 또는 과거 날짜여야 합니다.")
        LocalDate baseDate
) {
}