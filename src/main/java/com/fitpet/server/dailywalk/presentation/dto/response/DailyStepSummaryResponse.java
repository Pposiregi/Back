package com.fitpet.server.dailywalk.presentation.dto.response;

import com.fitpet.server.dailywalk.application.dto.DailyStepSummaryResult;
import java.time.LocalDate;

public record DailyStepSummaryResponse(
        LocalDate date,
        int step
) {
    public static DailyStepSummaryResponse from(DailyStepSummaryResult r) {
        return new DailyStepSummaryResponse(r.date(), r.step());
    }
}