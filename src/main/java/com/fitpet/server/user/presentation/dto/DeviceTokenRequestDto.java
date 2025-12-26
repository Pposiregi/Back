package com.fitpet.server.user.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "FCM 디바이스 토큰 등록/갱신 요청 DTO")
public record DeviceTokenRequestDto(

        @Schema(description = "기기 고유 식별자 (UUID)")
        @NotBlank(message = "기기 고유 ID(UUID)는 필수입니다.")
        String deviceUuid,

        @Schema(description = "FCM 토큰")
        @NotBlank(message = "디바이스 토큰은 필수입니다.")
        String deviceToken,

        @Schema(description = "기기 OS (ANDROID / IOS)", example = "ANDROID")
        @NotBlank(message = "기기 OS 정보는 필수입니다.")
        String deviceOs
) {
}