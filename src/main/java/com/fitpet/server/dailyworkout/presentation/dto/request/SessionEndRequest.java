package com.fitpet.server.dailyworkout.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
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

    private BigDecimal totalDistance;

    private Integer burnCalories;
}