package com.fitpet.server.ranking.infra.messaging;

import com.fitpet.server.ranking.application.dto.OvertakeMessage;
import com.fitpet.server.shared.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Dead Letter Queue 리스너.
 *
 * <p>3회 재시도 후에도 FCM 발송에 실패한 메시지는 DLQ 로 이동한다.
 * 현재는 에러 로그만 남기지만, 향후 Slack/PagerDuty 알림, 재처리 배치 등으로 확장 가능하다.</p>
 *
 * <h2>데드 큐 처리 전략</h2>
 * <ul>
 *   <li>메시지 내용을 로그로 남겨 운영팀이 확인할 수 있게 한다.</li>
 *   <li>FCM 토큰이 만료된 경우 → User 테이블에서 토큰 삭제하는 배치로 연계 가능.</li>
 *   <li>RabbitMQ 서버 오류인 경우 → 수동으로 DLQ 메시지를 원래 큐로 re-publish 한다.</li>
 * </ul>
 */
@Slf4j
@Component
public class DlqEventListener {

    @RabbitListener(queues = RabbitMQConfig.DLQ)
    public void handleDeadLetter(OvertakeMessage message) {
        log.error("[DLQ] 최종 처리 실패 메시지 수신 – 수동 확인 필요: outboxId={}, overtakenUserId={}, overtakingUserId={}",
                message.getOutboxId(), message.getOvertakenUserId(), message.getOvertakingUserId());
    }
}
