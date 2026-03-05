package com.fitpet.server.dailywalk.presentation.dto.response;

import com.fitpet.server.dailywalk.application.dto.DailyWalkResult;
import com.fitpet.server.dailywalk.domain.entity.DailyWalk;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DailyWalkResponse(
        Long id,
        Integer step,
        BigDecimal distanceKm,
        Integer burnCalories,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static DailyWalkResponse from(DailyWalk e) {
        return new DailyWalkResponse(
                e.getId(),
                e.getStep(),
                e.getDistanceKm(),
                e.getBurnCalories(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    public static DailyWalkResponse from(DailyWalkResult r) {
        return new DailyWalkResponse(
                r.id(),
                r.step(),
                r.distanceKm(),
                r.burnCalories(),
                r.createdAt(),
                r.updatedAt()
        );
    }
}
