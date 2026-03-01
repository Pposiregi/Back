package com.fitpet.server.user.presentation.dto;

import com.fitpet.server.user.application.dto.UserResult;
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
    PetSummaryDto pet,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public UserDto withPet(PetSummaryDto pet) {
        return new UserDto(
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

    public static UserDto from(UserResult result) {
        if (result == null) {
            return null;
        }

        return UserDto.builder()
            .userId(result.userId())
            .email(result.email())
            .nickname(result.nickname())
            .profileImageUrl(result.profileImageUrl())
            .age(result.age())
            .gender(result.gender())
            .weightKg(result.weightKg())
            .targetWeightKg(result.targetWeightKg())
            .heightCm(result.heightCm())
            .pbf(result.pbf())
            .targetPbf(result.targetPbf())
            .targetStepCount(result.targetStepCount())
            .dailyStepCount(result.dailyStepCount())
            .pet(PetSummaryDto.from(result.pet()))
            .createdAt(result.createdAt())
            .updatedAt(result.updatedAt())
            .build();
    }
}
