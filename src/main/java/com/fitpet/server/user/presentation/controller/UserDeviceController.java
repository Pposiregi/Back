package com.fitpet.server.user.presentation.controller;

import com.fitpet.server.shared.annotation.AuthUser;
import com.fitpet.server.user.application.dto.DeviceTokenCommand;
import com.fitpet.server.user.application.service.UserDeviceService;
import com.fitpet.server.user.presentation.dto.DeviceDeleteRequestDto;
import com.fitpet.server.user.presentation.dto.DeviceTokenRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/devices")
@RequiredArgsConstructor
@Tag(name = "User Device", description = "사용자 디바이스 및 FCM 토큰 관리 API")
public class UserDeviceController {

    private final UserDeviceService userDeviceService;

    @Operation(summary = "디바이스 토큰 등록/갱신", description = "로그인 또는 앱 실행 시 디바이스 정보를 등록하거나 최신 토큰으로 갱신합니다.")
    @PostMapping("/push-token")
    public ResponseEntity<String> registerDeviceToken(
            @AuthUser Long userId,
            @Valid @RequestBody DeviceTokenRequestDto requestDto
    ) {
        DeviceTokenCommand command = DeviceTokenCommand.of(
                requestDto.deviceUuid(),
                requestDto.deviceToken(),
                requestDto.deviceOs()
        );

        userDeviceService.registerDevice(userId, command);

        return ResponseEntity.ok("디바이스 토큰이 성공적으로 등록되었습니다.");
    }

    @Operation(summary = "디바이스 토큰 단순 갱신", description = "앱 실행 중 토큰이 변경되었을 때 갱신합니다.")
    @PatchMapping("/push-token")
    public ResponseEntity<String> refreshDeviceToken(
            @AuthUser Long userId,
            @Valid @RequestBody DeviceTokenRequestDto requestDto
    ) {
        DeviceTokenCommand command = DeviceTokenCommand.of(
                requestDto.deviceUuid(),
                requestDto.deviceToken(),
                requestDto.deviceOs()
        );

        userDeviceService.refreshDeviceToken(userId, command);

        return ResponseEntity.ok("디바이스 토큰이 갱신되었습니다.");
    }

    @Operation(summary = "디바이스 알림 비활성화", description = "로그아웃 시 해당 기기의 알림 수신을 중지합니다.")
    @DeleteMapping("/push-token")
    public ResponseEntity<Void> deactivateDevice(
            @AuthUser Long userId,
            @RequestBody @Valid DeviceDeleteRequestDto request
    ) {
        userDeviceService.deactivateDevice(userId, request.deviceUuid());

        return ResponseEntity.noContent().build();
    }
}