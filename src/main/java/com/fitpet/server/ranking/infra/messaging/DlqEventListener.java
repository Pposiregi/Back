package com.fitpet.server.ranking.infra.messaging;

import com.fitpet.server.ranking.application.dto.OvertakeMessage;
import com.fitpet.server.shared.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DlqEventListener {

    @RabbitListener(queues = RabbitMQConfig.DLQ)
    public void handleDeadLetter(OvertakeMessage message) {
        log.error("[DLQ] 최종 처리 실패 메시지 수신 – 수동 확인 필요: outboxId={}, overtakenUserId={}, overtakingUserId={}",
                message.getOutboxId(), message.getOvertakenUserId(), message.getOvertakingUserId());
    }
}
