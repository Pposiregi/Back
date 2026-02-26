package com.fitpet.server.dailyworkout.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SessionStartRequest {
    @NotNull
    private LocalDateTime startTime;
}