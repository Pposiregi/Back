package com.fitpet.server.mission.infra.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitpet.server.mission.application.dto.MissionProgressEvent;
import com.fitpet.server.mission.application.service.MissionProgressStreamService;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MissionProgressRedisSubscriber implements MessageListener {

    private static final String CONSUME_METRIC = "mission.progress.consume.total";

    private final ObjectMapper objectMapper;
    private final MissionProgressStreamService missionProgressStreamService;
    private final MeterRegistry meterRegistry;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        meterRegistry.counter(CONSUME_METRIC, "status", "received").increment();

        MissionProgressEvent event;
        try {
            event = objectMapper.readValue(payload, MissionProgressEvent.class);
        } catch (JsonProcessingException e) {
            meterRegistry.counter(CONSUME_METRIC, "status", "decode_failure").increment();
            log.warn("[MissionProgressRedisSubscriber] 이벤트 역직렬화 실패: payloadSize={}", payload.length(), e);
            return;
        }

        try {
            missionProgressStreamService.emitMissionProgress(event);
            meterRegistry.counter(CONSUME_METRIC, "status", "processed").increment();
        } catch (Exception e) {
            meterRegistry.counter(CONSUME_METRIC, "status", "emit_failure").increment();
            log.warn("[MissionProgressRedisSubscriber] 이벤트 전달 실패: eventId={}", event.eventId(), e);
        }
    }
}
