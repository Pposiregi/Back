package com.fitpet.server.user.presentation.dto.request;

import com.fitpet.server.user.application.dto.UserUpdateCommand;
import com.fitpet.server.user.domain.entity.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @Email
        String email,

        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,100}$",
                message = "비밀번호는 영어, 숫자, 특수문자를 포함해야 합니다."
        )
        String password,
        @Size(min = 2, max = 10)
        String nickname,
        Integer age,
        Gender gender,
        Double weightKg,
        Double targetWeightKg,
        Double heightCm,
        Double pbf,
        Double targetPbf,
        Integer targetStepCount
) {
    public UserUpdateCommand toCommand() {
        return new UserUpdateCommand(
            email,
            password,
            nickname,
            age,
            gender,
            weightKg,
            targetWeightKg,
            heightCm,
            pbf,
            targetPbf,
            targetStepCount
        );
    }
}
