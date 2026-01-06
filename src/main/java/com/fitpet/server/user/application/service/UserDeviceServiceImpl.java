package com.fitpet.server.user.application.service;

import com.fitpet.server.shared.exception.BusinessException;
import com.fitpet.server.shared.exception.ErrorCode;
import com.fitpet.server.user.application.dto.DeviceTokenCommand;
import com.fitpet.server.user.domain.entity.User;
import com.fitpet.server.user.domain.entity.UserDevice;
import com.fitpet.server.user.domain.repository.UserDeviceRepository;
import com.fitpet.server.user.domain.repository.UserRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDeviceServiceImpl implements UserDeviceService {

    private final UserDeviceRepository userDeviceRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void registerDevice(Long userId, DeviceTokenCommand command) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // upsert
        userDeviceRepository.findByUserAndDeviceUuid(user, command.getDeviceUuid())
                .ifPresentOrElse(
                        existingDevice -> {
                            log.info("기존 기기 토큰 갱신: userId={}, uuid={}", userId, command.getDeviceUuid());
                            existingDevice.loginSuccess(command.getDeviceToken());
                        },
                        () -> {
                            log.info("새 기기 등록: userId={}, uuid={}", userId, command.getDeviceUuid());
                            userDeviceRepository.save(UserDevice.builder()
                                    .user(user)
                                    .deviceUuid(command.getDeviceUuid())
                                    .deviceToken(command.getDeviceToken())
                                    .deviceOs(command.getDeviceOs())
                                    .lastLoginAt(LocalDateTime.now())
                                    .deleted(false)
                                    .build());
                        }
                );
    }

    @Override
    @Transactional
    public void refreshDeviceToken(Long userId, DeviceTokenCommand command) {
        User user = userRepository.getReferenceById(userId);

        UserDevice device = userDeviceRepository.findByUserAndDeviceUuid(user, command.getDeviceUuid())
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));

        device.updateToken(command.getDeviceToken());
        log.info("토큰 Refresh 완료: userId={}, uuid={}", userId, command.getDeviceUuid());
    }

    @Override
    @Transactional
    public void deactivateDevice(Long userId, String deviceUuid) {
        User user = userRepository.getReferenceById(userId);

        userDeviceRepository.findByUserAndDeviceUuid(user, deviceUuid)
                .ifPresent(device -> {
                    device.disconnect();
                    log.info("기기 비활성화(로그아웃): userId={}, uuid={}", userId, deviceUuid);
                });
    }
}