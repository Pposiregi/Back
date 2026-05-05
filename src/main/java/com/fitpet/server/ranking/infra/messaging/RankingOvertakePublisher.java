package com.fitpet.server.ranking.infra.messaging;

import com.fitpet.server.ranking.application.dto.OvertakeMessage;
import com.fitpet.server.ranking.domain.entity.RankingOvertakeOutbox;
import com.fitpet.server.shared.config.RabbitMQConfig;
import com.fitpet.server.user.domain.entity.User;
import com.fitpet.server.user.domain.repository.UserRepository;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingOvertakePublisher {

    private static final long CONFIRM_TIMEOUT_SECONDS = 5;

    private final RabbitTemplate rabbitTemplate;
    private final UserRepository userRepository;

    public void publish(RankingOvertakeOutbox outbox) {
        String nickname = userRepository.findById(outbox.getOvertakingUserId())
                .map(User::getNickname)
                .orElse("누군가");

        OvertakeMessage message = OvertakeMessage.builder()
                .outboxId(outbox.getId())
                .overtakenUserId(outbox.getOvertakenUserId())
                .overtakingUserId(outbox.getOvertakingUserId())
                .overtakingUserNickname(nickname)
                .dateKey(outbox.getDateKey())
                .overtakerCount(outbox.getOvertakerCount())
                .build();

        CorrelationData correlationData = new CorrelationData(String.valueOf(outbox.getId()));

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.ROUTING_KEY,
                message,
                correlationData
        );

        waitForConfirm(outbox.getId(), correlationData);

        log.debug("[OvertakePublisher] 메시지 발행 완료 (ACK): outboxId={}, overtakenUserId={}",
                outbox.getId(), outbox.getOvertakenUserId());
    }

    private void waitForConfirm(Long outboxId, CorrelationData correlationData) {
        try {
            CorrelationData.Confirm confirm =
                    correlationData.getFuture().get(CONFIRM_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (confirm == null || !confirm.isAck()) {
                String reason = (confirm != null) ? confirm.getReason() : "confirm null";
                throw new RuntimeException(
                        "[OvertakePublisher] 브로커 NACK – 재시도 예정: outboxId=" + outboxId + ", reason=" + reason);
            }

        } catch (TimeoutException e) {
            throw new RuntimeException(
                    "[OvertakePublisher] 브로커 ACK 타임아웃 – 재시도 예정: outboxId=" + outboxId, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(
                    "[OvertakePublisher] 브로커 ACK 대기 중 인터럽트: outboxId=" + outboxId, e);
        } catch (ExecutionException e) {
            throw new RuntimeException(
                    "[OvertakePublisher] 브로커 ACK 대기 중 예외: outboxId=" + outboxId, e.getCause());
        }
    }
}
