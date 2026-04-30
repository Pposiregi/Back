package com.fitpet.server.ranking.infra.messaging;

import com.fitpet.server.alram.domain.entity.AlramMessage;
import com.fitpet.server.alram.domain.repository.AlramRepository;
import com.fitpet.server.ranking.application.dto.OvertakeMessage;
import com.fitpet.server.shared.config.RabbitMQConfig;
import com.fitpet.server.user.domain.entity.User;
import com.fitpet.server.user.domain.repository.UserDeviceRepository;
import com.fitpet.server.user.domain.repository.UserRepository;
import java.util.List;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 랭킹 추월 알림 MQ 소비자.
 *
 * <h2>처리 흐름</h2>
 * <ol>
 *   <li>수신된 메시지에서 피추월자 정보를 조회한다.</li>
 *   <li>알림 수신 동의 및 디바이스 토큰을 확인한다.</li>
 *   <li>FCM 푸시 알림을 발송한다.</li>
 *   <li>알림 발송 이력을 DB에 저장한다.</li>
 * </ol>
 *
 * <h2>실패 처리</h2>
 * <p>FCM 예외 발생 시 {@link RuntimeException} 으로 재포장하면 Spring AMQP 가
 * {@link RabbitMQConfig#rankingOvertakeRetryInterceptor()} 정책에 따라 재시도한다.
 * 3회 초과 시 Dead Letter Queue 로 이동한다.</p>
 *
 * <h2>멱등성</h2>
 * <p>Rate Limiter(Redis)가 아웃박스 저장 시점에 이미 중복 발송을 차단하므로
 * 소비자 레벨에서는 추가 멱등성 처리 없이 단순하게 유지한다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RankingOvertakeConsumer {

    private static final String NOTIFICATION_TITLE = "랭킹 변동 알림 🏃";

    private final UserRepository userRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final FirebaseMessaging firebaseMessaging;
    private final AlramRepository alramRepository;

    @RabbitListener(queues = RabbitMQConfig.QUEUE, containerFactory = "rankingOvertakeListenerFactory")
    @Transactional
    public void consume(OvertakeMessage message) {
        log.info("[OvertakeConsumer] 처리 시작: outboxId={}, overtakenUserId={}",
                message.getOutboxId(), message.getOvertakenUserId());

        User overtakenUser = userRepository.findById(message.getOvertakenUserId()).orElse(null);
        if (overtakenUser == null) {
            log.warn("[OvertakeConsumer] 사용자 없음 – 메시지 폐기: userId={}", message.getOvertakenUserId());
            return; // ack (폐기)
        }

        // 알림 수신 동의 확인
        if (!Boolean.TRUE.equals(overtakenUser.getAllowActivityNotification())) {
            log.info("[OvertakeConsumer] 알림 수신 거부 – 건너뜀: userId={}", message.getOvertakenUserId());
            return; // ack (폐기)
        }

        List<String> deviceTokens = userDeviceRepository.findAllTokensByUserId(message.getOvertakenUserId());
        if (deviceTokens.isEmpty()) {
            log.warn("[OvertakeConsumer] 디바이스 토큰 없음 – 건너뜀: userId={}", message.getOvertakenUserId());
            return; // ack (폐기)
        }

        String body = message.getOvertakerCount() + "명에게 순위를 추월당했어요! 달려볼까요? 🏃";

        deviceTokens.forEach(token -> sendFcm(token, body, message));
        saveAlramHistory(overtakenUser, body);

        log.info("[OvertakeConsumer] 처리 완료: outboxId={}", message.getOutboxId());
    }

    private void sendFcm(String deviceToken, String body, OvertakeMessage message) {
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
            // 예외를 던져야 Spring AMQP 가 재시도 → 최종적으로 DLQ 로 이동한다.
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
