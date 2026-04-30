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

/**
 * 아웃박스 이벤트를 RabbitMQ 로 발행하는 컴포넌트.
 *
 * <p>폴러({@link com.fitpet.server.ranking.infra.scheduler.OvertakeOutboxPollerScheduler})가
 * CLAIMED 이벤트 배치를 가져와 이 클래스를 통해 메시지를 발행한다.</p>
 *
 * <h2>Publisher Confirm (브로커 확인)</h2>
 * <p>{@code publisher-confirm-type: CORRELATED} 설정 하에 {@link CorrelationData}(outboxId)를
 * 함께 전달하고, 브로커의 ACK/NACK 를 {@link CorrelationData#getFuture()} 로 동기 대기한다.
 * 브로커가 ACK 를 보내야 정상 반환되며, NACK·타임아웃 시 예외를 던져 폴러가
 * retryCount 를 증가시키고 PENDING 으로 복원하도록 유도한다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RankingOvertakePublisher {

    private static final long CONFIRM_TIMEOUT_SECONDS = 5;

    private final RabbitTemplate rabbitTemplate;
    private final UserRepository userRepository;

    /**
     * 아웃박스 이벤트 한 건을 MQ 로 발행하고 브로커 ACK 를 동기 대기한다.
     *
     * @param outbox CLAIMED 상태의 아웃박스 이벤트
     * @throws RuntimeException 브로커 NACK·타임아웃·인터럽트 발생 시
     */
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

        // outboxId를 상관관계 ID로 사용해 브로커 ACK/NACK 를 추적한다
        CorrelationData correlationData = new CorrelationData(String.valueOf(outbox.getId()));

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.ROUTING_KEY,
                message,
                correlationData
        );

        // 브로커 ACK 를 동기적으로 대기 – 미확인 시 예외를 던져 폴러가 재시도하도록 유도
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
