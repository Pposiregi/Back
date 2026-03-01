package com.fitpet.server.user.presentation.dto.request;

import com.fitpet.server.user.application.dto.UserInputInfoCommand;
import com.fitpet.server.user.domain.entity.Gender;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UserInputInfoRequest(
        @NotBlank(message = "닉네임은 공백일 수 없습니다.")
        @Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하여야 합니다.")
        String nickname,

        @Min(value = 1, message = "나이는 1세 이상이어야 합니다.")
        @Max(value = 120, message = "나이는 120세 이하여야 합니다.")
        int age,

        @NotNull
        Gender gender,

        @NotNull
        @Positive(message = "몸무게(kg)는 0보다 커야 합니다.")
        Double weightKg,

        @NotNull
        @Positive(message = "키(cm)는 0보다 커야 합니다.")
        Double heightCm,

        Double targetWeightKg,

        Double pbf,

        Double targetPbf,

        Integer targetStepCount
) {
    public UserInputInfoCommand toCommand() {
        return new UserInputInfoCommand(
            nickname,
            age,
            gender,
            weightKg,
            heightCm,
            targetWeightKg,
            pbf,
            targetPbf,
            targetStepCount
        );
    }
}
