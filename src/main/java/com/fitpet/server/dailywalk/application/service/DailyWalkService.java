package com.fitpet.server.dailywalk.application.service;

import com.fitpet.server.dailywalk.application.dto.DailyStepSummaryResult;
import com.fitpet.server.dailywalk.application.dto.DailyWalkCreateCommand;
import com.fitpet.server.dailywalk.application.dto.DailyWalkResult;
import com.fitpet.server.dailywalk.application.dto.DailyWalkStepUpdateCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;
import java.util.List;
import org.springframework.validation.annotation.Validated;

@Validated
public interface DailyWalkService {

    List<DailyWalkResult> getAllByUserId(@NotNull Long userId);

    DailyWalkResult getDailyWalkByUserIdAndDate(
            @NotNull Long userId,
            @NotNull @PastOrPresent LocalDate date
    );

    List<DailyStepSummaryResult> getWeeklySteps(@NotNull Long userId);

    DailyWalkResult createDailyWalk(@NotNull Long userId, @NotNull DailyWalkCreateCommand cmd);

    void updateDailyWalkStep(@NotNull Long userId, @NotNull DailyWalkStepUpdateCommand cmd);

    void deleteDailyWalk(@NotNull Long userId, @NotNull Long dailyWalkId);
}
