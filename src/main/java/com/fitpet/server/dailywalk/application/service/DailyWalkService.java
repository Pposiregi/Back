package com.fitpet.server.dailywalk.application.service;

import com.fitpet.server.dailywalk.presentation.dto.request.DailyWalkCreateRequest;
import com.fitpet.server.dailywalk.presentation.dto.request.DailyWalkStepUpdateRequest;
import com.fitpet.server.dailywalk.presentation.dto.response.DailyStepSummaryResponse;
import com.fitpet.server.dailywalk.presentation.dto.response.DailyWalkResponse;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;
import java.util.List;
import org.springframework.validation.annotation.Validated;

@Validated
public interface DailyWalkService {
    List<DailyWalkResponse> getAllByUserId(@NotNull Long userId);

    DailyWalkResponse getDailyWalkByUserIdAndDate(
            @NotNull Long userId,
            @NotNull @PastOrPresent LocalDate date
    );

    List<DailyStepSummaryResponse> getWeeklySteps(@NotNull Long userId);

    DailyWalkResponse createDailyWalk(@NotNull Long userId, DailyWalkCreateRequest req);

    void deleteDailyWalk(@NotNull Long userId, @NotNull Long dailyWalkId);

    void updateDailyWalkStep(
            @NotNull Long userId,
            DailyWalkStepUpdateRequest req
    );
}
