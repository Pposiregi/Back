package com.fitpet.server.dailyworkout.presentation.dto.response;

import java.math.BigDecimal;

public record GpsRouteLogResponse(
    BigDecimal latitude,
    BigDecimal longitude,
    BigDecimal altitude
) {
    public static GpsRouteLogResponse of(BigDecimal latitude, BigDecimal longitude, BigDecimal altitude) {
        return new GpsRouteLogResponse(latitude, longitude, altitude);
    }
}