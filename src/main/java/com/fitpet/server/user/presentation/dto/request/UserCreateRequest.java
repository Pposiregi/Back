package com.fitpet.server.user.presentation.dto.request;

import com.fitpet.server.user.domain.entity.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record UserCreateRequest(
        @NotBlank
        @Email
        String email,

        @NotBlank
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,100}$",
                message = "비밀번호는 영어, 숫자, 특수문자를 포함해야 합니다."
        )
        String password,

        @NotBlank
        @Size(min = 2, max = 10)
        String nickname,

        @NotNull
        Integer age,

        @NotNull
        Gender gender,

        @NotNull
        Double weightKg,

        @NotNull
        Double targetWeightKg,

        @NotNull
        Double heightCm,

        @NotNull
        Double pbf,

        @NotNull
        Double targetPbf,

        @NotNull
        Integer targetStepCount
) {
}