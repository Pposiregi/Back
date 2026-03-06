package com.fitpet.server.mission.infra.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitpet.server.mission.application.dto.MissionProgressEvent;
import com.fitpet.server.mission.application.service.MissionProgressEventPublisher;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class MissionProgressRedisPublisher implements MissionProgressEventPublisher {

    private static final String PUBLISH_METRIC = "mission.progress.publish.total";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    @Override
    public void publishAfterCommit(MissionProgressEvent event) {
        if (isTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishNow(event);
                }
            });
            return;
        }
        publishNow(event);
    }

    private boolean isTransactionActive() {
        return TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive();
    }

    private void publishNow(MissionProgressEvent event) {
        String eventType = event.eventType().name();
        try {
            String payload = objectMapper.writeValueAsString(event);
            stringRedisTemplate.convertAndSend(MissionProgressRedisChannels.MISSION_PROGRESS, payload);
            meterRegistry.counter(PUBLISH_METRIC, "event_type", eventType, "status", "success").increment();
        } catch (JsonProcessingException e) {
            meterRegistry.counter(PUBLISH_METRIC, "event_type", eventType, "status", "failure").increment();
            log.warn("[MissionProgressRedisPublisher] 이벤트 직렬화 실패: eventId={}", event.eventId(), e);
        }
    }
}
