package com.fitpet.server.user.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record DeviceDeleteRequestDto(
        @Schema(description = "기기 고유 식별자 (UUID)")
        @NotBlank(message = "기기 고유 ID는 필수입니다.")
        String deviceUuid
) {
}