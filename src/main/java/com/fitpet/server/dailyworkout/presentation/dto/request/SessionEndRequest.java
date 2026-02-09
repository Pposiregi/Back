package com.fitpet.server.dailyworkout.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SessionEndRequest {

    @NotNull
    private Long sessionId;

    @NotNull
    private LocalDateTime endTime;

    @NotNull
    private Integer stepCount;

    // @NotNull
    // private BigDecimal distance;

    // private Integer totalDistance;

    private Integer burnCalories;
}