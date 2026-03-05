package com.fitpet.server.dailywalk.application.dto;

import java.time.LocalDate;

public record DailyStepSummaryResult(
        LocalDate date,
        int step
) {
}
