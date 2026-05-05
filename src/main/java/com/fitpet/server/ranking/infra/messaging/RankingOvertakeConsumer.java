package com.fitpet.server.ranking.infra.messaging;

import com.fitpet.server.alram.domain.entity.AlramMessage;
import com.fitpet.server.alram.domain.repository.AlramRepository;
import com.fitpet.server.ranking.application.dto.OvertakeMessage;
import com.fitpet.server.shared.config.RabbitMQConfig;
import com.fitpet.server.user.domain.entity.User;
import com.fitpet.server.user.domain.repository.UserDeviceRepository;
import com.fitpet.server.user.domain.repository.UserRepository;
import java.time.Duration;
import java.util.List;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingOvertakeConsumer {

    private static final String NOTIFICATION_TITLE = "랭킹 변동 알림 🏃";
    private static final String FCM_IDEM_KEY_PREFIX = "fcm:idem:";
    private static final Duration FCM_IDEM_TTL = Duration.ofHours(1);

    private final UserRepository userRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final FirebaseMessaging firebaseMessaging;
    private final AlramRepository alramRepository;
    private final StringRedisTemplate stringRedisTemplate;

    @RabbitListener(queues = RabbitMQConfig.QUEUE, containerFactory = "rankingOvertakeListenerFactory")
    @Transactional
    public void consume(OvertakeMessage message) {
        log.info("[OvertakeConsumer] 처리 시작: outboxId={}, overtakenUserId={}",
                message.getOutboxId(), message.getOvertakenUserId());

        User overtakenUser = userRepository.findById(message.getOvertakenUserId()).orElse(null);
        if (overtakenUser == null) {
            log.warn("[OvertakeConsumer] 사용자 없음 – 메시지 폐기: userId={}", message.getOvertakenUserId());
            return;
        }

        if (!Boolean.TRUE.equals(overtakenUser.getAllowActivityNotification())) {
            log.info("[OvertakeConsumer] 알림 수신 거부 – 건너뜀: userId={}", message.getOvertakenUserId());
            return;
        }

        List<String> deviceTokens = userDeviceRepository.findAllTokensByUserId(message.getOvertakenUserId());
        if (deviceTokens.isEmpty()) {
            log.warn("[OvertakeConsumer] 디바이스 토큰 없음 – 건너뜀: userId={}", message.getOvertakenUserId());
            return;
        }

        String body = message.getOvertakerCount() + "명에게 순위를 추월당했어요! 달려볼까요? 🏃";

        deviceTokens.forEach(token -> sendFcm(token, body, message));
        saveAlramHistory(overtakenUser, body);

        log.info("[OvertakeConsumer] 처리 완료: outboxId={}", message.getOutboxId());
    }

    private void sendFcm(String deviceToken, String body, OvertakeMessage message) {
        String idemKey = FCM_IDEM_KEY_PREFIX + message.getOutboxId() + ":" + deviceToken;
        Boolean isNew = stringRedisTemplate.opsForValue().setIfAbsent(idemKey, "1", FCM_IDEM_TTL);
        if (!Boolean.TRUE.equals(isNew)) {
            log.info("[OvertakeConsumer] 이미 발송된 토큰 – 중복 발송 건너뜀: outboxId={}", message.getOutboxId());
            return;
        }

        Message fcmMessage = Message.builder()
                .setToken(deviceToken)
                .setNotification(Notification.builder()
                        .setTitle(NOTIFICATION_TITLE)
                        .setBody(body)
                        .build())
                .putData("type", "RANKING_OVERTAKE")
                .putData("overtakingUserId", String.valueOf(message.getOvertakingUserId()))
                .putData("dateKey", message.getDateKey())
                .build();

        try {
            String fcmId = firebaseMessaging.send(fcmMessage);
            log.info("[OvertakeConsumer] FCM 발송 성공: messageId={}, overtakenUserId={}",
                    fcmId, message.getOvertakenUserId());
        } catch (FirebaseMessagingException e) {
            stringRedisTemplate.delete(idemKey);
            log.error("[OvertakeConsumer] FCM 발송 실패: overtakenUserId={}", message.getOvertakenUserId(), e);
            throw new RuntimeException("FCM 발송 실패 – 재시도 예정", e);
        }
    }

    @Transactional
    protected void saveAlramHistory(User overtakenUser, String body) {
        alramRepository.save(
                AlramMessage.builder()
                        .user(overtakenUser)
                        .title(NOTIFICATION_TITLE)
                        .message(body)
                        .build()
        );
    }
}
