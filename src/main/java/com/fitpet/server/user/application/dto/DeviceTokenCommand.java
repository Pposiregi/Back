package com.fitpet.server.user.application.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DeviceTokenCommand {
    private String deviceUuid;
    private String deviceToken;
    private String deviceOs;

    public static DeviceTokenCommand of(String deviceUuid, String deviceToken, String deviceOs) {
        return DeviceTokenCommand.builder()
                .deviceUuid(deviceUuid)
                .deviceToken(deviceToken)
                .deviceOs(deviceOs)
                .build();
    }
}