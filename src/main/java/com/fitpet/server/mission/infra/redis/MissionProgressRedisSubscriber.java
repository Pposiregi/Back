package com.fitpet.server.mission.infra.redis;

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
        try {
            MissionProgressEvent event = objectMapper.readValue(payload, MissionProgressEvent.class);
            missionProgressStreamService.emitMissionProgress(event);
            meterRegistry.counter(CONSUME_METRIC, "status", "processed").increment();
        } catch (Exception e) {
            meterRegistry.counter(CONSUME_METRIC, "status", "decode_failure").increment();
            log.warn("[MissionProgressRedisSubscriber] 이벤트 역직렬화 실패: payload={}", payload, e);
        }
    }
}
