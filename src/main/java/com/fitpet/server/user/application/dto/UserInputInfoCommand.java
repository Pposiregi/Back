package com.fitpet.server.user.application.dto;

import com.fitpet.server.user.domain.entity.Gender;

public record UserInputInfoCommand(
    String nickname,
    int age,
    Gender gender,
    Double weightKg,
    Double heightCm,
    Double targetWeightKg,
    Double pbf,
    Double targetPbf,
    Integer targetStepCount
) {
}
