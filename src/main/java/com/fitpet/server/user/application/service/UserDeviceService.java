package com.fitpet.server.user.application.service;

import com.fitpet.server.user.application.dto.DeviceTokenCommand;

public interface UserDeviceService {

    void registerDevice(Long userId, DeviceTokenCommand command);

    void refreshDeviceToken(Long userId, DeviceTokenCommand command);

    void deactivateDevice(Long userId, String deviceUuid);
}