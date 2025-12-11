package com.fitpet.server.alram.application.service;

import com.fitpet.server.alram.application.mapper.AlramMapper;
import com.fitpet.server.alram.domain.entity.AlramMessage;
import com.fitpet.server.alram.domain.repository.AlramRepository;
import com.fitpet.server.alram.presentation.dto.request.AlramRequestDto;
import com.fitpet.server.alram.presentation.dto.response.AlramResponseDto;
import com.fitpet.server.shared.exception.BusinessException;
import com.fitpet.server.shared.exception.ErrorCode;
import com.fitpet.server.user.domain.entity.User;
import com.fitpet.server.user.domain.repository.UserRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlramServiceImpl implements AlramService {

    private final FirebaseMessaging firebaseMessaging;
    private final UserRepository userRepository;
    private final AlramRepository alramRepository;
    private final AlramMapper alramMapper;

    @Override
    @Transactional
    public AlramResponseDto sendAndSaveAlram(AlramRequestDto requestDto) {
        log.info("[AlramService] FCM 알림 전송 시작");

        User user = userRepository.findById(requestDto.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String deviceToken = user.getDeviceToken();
        log.info("[AlramService] 사용자(ID: {}) 디바이스 토큰 확인 완료", user.getId());

        if (deviceToken == null || deviceToken.isBlank()) {
            log.warn("[AlramService] FCM 알림 발송 실패: 사용자(ID: {})의 디바이스 토큰이 없습니다.", user.getId());
            throw new BusinessException(ErrorCode.DEVICE_TOKEN_NOT_FOUND);
        }

        Notification notification = alramMapper.toNotification(requestDto);

        Message.Builder fcmMessageBuilder = Message.builder()
                .setToken(deviceToken)
                .setNotification(notification);

        if (requestDto.getData() != null && !requestDto.getData().isEmpty()) {
            fcmMessageBuilder.putAllData(requestDto.getData());
        }

        Message fcmMessage = fcmMessageBuilder.build();
        String fcmMessageId;

        try {
            fcmMessageId = firebaseMessaging.send(fcmMessage);
            log.info("[AlramService] FCM 알림 발송 성공. Message ID: {}", fcmMessageId);
        } catch (FirebaseMessagingException e) {
            log.error("[AlramService] FCM 알림 발송 실패: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.FCM_SEND_FAILED);
        }

        AlramMessage alramToSave = alramMapper.toAlramMessage(requestDto, user);
        alramToSave.setUser(user);

        AlramMessage savedAlram = alramRepository.save(alramToSave);
        log.info("[AlramService] 알림 발송 내역 저장 완료 (ID: {})", savedAlram.getId());

        return alramMapper.toAlramResponseDto(
                savedAlram,
                fcmMessageId,
                "알림 발송 및 저장 성공",
                requestDto.getData()
        );
    }
}