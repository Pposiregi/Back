package com.fitpet.server.user.presentation.dto;

import com.fitpet.server.user.domain.entity.Gender;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record UserDto(
        Long userId,
        String email,
        String nickname,
        String profileImageUrl,
        Integer age,
        Gender gender,
        Double weightKg,
        Double targetWeightKg,
        Double heightCm,
        Double pbf,
        Double targetPbf,
        Integer targetStepCount,
        Integer dailyStepCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}