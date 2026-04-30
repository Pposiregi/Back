package com.fitpet.server.ranking.infra.messaging;

import com.fitpet.server.ranking.application.dto.OvertakeMessage;
import com.fitpet.server.ranking.domain.entity.RankingOvertakeOutbox;
import com.fitpet.server.shared.config.RabbitMQConfig;
import com.fitpet.server.user.domain.entity.User;
import com.fitpet.server.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 아웃박스 이벤트를 RabbitMQ 로 발행하는 컴포넌트.
 *
 * <p>폴러({@link com.fitpet.server.ranking.infra.scheduler.OvertakeOutboxPollerScheduler})가
 * PENDING 이벤트 배치를 가져와 이 클래스를 통해 메시지를 발행한다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RankingOvertakePublisher {

    private final RabbitTemplate rabbitTemplate;
    private final UserRepository userRepository;

    /**
     * 아웃박스 이벤트 한 건을 MQ 로 발행한다.
     *
     * @param outbox PENDING 상태의 아웃박스 이벤트
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

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.ROUTING_KEY,
                message
        );

        log.debug("[OvertakePublisher] 메시지 발행: outboxId={}, overtakenUserId={}",
                outbox.getId(), outbox.getOvertakenUserId());
    }
}
