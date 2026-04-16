package com.fitpet.server.user.application.dto;

import com.fitpet.server.user.domain.entity.Gender;

public record UserUpdateCommand(
    String email,
    String password,
    String nickname,
    Integer age,
    Gender gender,
    Double weightKg,
    Double targetWeightKg,
    Double heightCm,
    Double pbf,
    Double targetPbf,
    Integer targetStepCount,
    String profileImageKey
) {
}
