package com.fitpet.server.dailyworkout.presentation.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class GpsLogResponse {
    private Long sessionId;
    private Long logId;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private LocalDateTime recordedAt;
    private String message;
}