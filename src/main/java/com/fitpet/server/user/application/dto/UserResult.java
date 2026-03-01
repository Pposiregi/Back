package com.fitpet.server.user.application.dto;

import com.fitpet.server.user.domain.entity.Gender;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record UserResult(
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
    PetSummaryResult pet,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public UserResult withPet(PetSummaryResult pet) {
        return new UserResult(
            userId,
            email,
            nickname,
            profileImageUrl,
            age,
            gender,
            weightKg,
            targetWeightKg,
            heightCm,
            pbf,
            targetPbf,
            targetStepCount,
            dailyStepCount,
            pet,
            createdAt,
            updatedAt
        );
    }

    public UserResult withProfileImageUrl(String profileImageUrl) {
        return new UserResult(
            userId,
            email,
            nickname,
            profileImageUrl,
            age,
            gender,
            weightKg,
            targetWeightKg,
            heightCm,
            pbf,
            targetPbf,
            targetStepCount,
            dailyStepCount,
            pet,
            createdAt,
            updatedAt
        );
    }
}
