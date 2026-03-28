package com.fitpet.server.mission.application.service;

import com.fitpet.server.mission.application.dto.MissionProgressEvent;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
public class MissionProgressStreamService {

    private static final long EMITTER_TIMEOUT_MS = 60L * 60L * 1000L;
    private static final long HEARTBEAT_INTERVAL_MS = 30_000L;
    private static final String DELIVERY_METRIC = "mission.progress.delivery.total";
    private static final String DELIVERY_LATENCY_METRIC = "mission.progress.delivery.latency";
    private static final String SUBSCRIPTION_METRIC = "mission.progress.sse.subscription.total";
    private static final String ACTIVE_EMITTER_METRIC = "mission.progress.sse.emitters.active";

    private final MeterRegistry meterRegistry;

    private final Map<Long, Map<String, SseEmitter>> emittersByUser = new ConcurrentHashMap<>();
    private final AtomicInteger activeEmitterCount = new AtomicInteger();

    public MissionProgressStreamService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        Gauge.builder(ACTIVE_EMITTER_METRIC, activeEmitterCount, AtomicInteger::get)
            .description("Number of active SSE emitters for mission progress stream")
            .register(meterRegistry);
    }

    public SseEmitter subscribe(Long userId) {
        String emitterId = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);

        emittersByUser
                .computeIfAbsent(userId, ignored -> new ConcurrentHashMap<>())
                .put(emitterId, emitter);
        activeEmitterCount.incrementAndGet();
        meterRegistry.counter(SUBSCRIPTION_METRIC).increment();

        emitter.onCompletion(() -> removeEmitter(userId, emitterId));
        emitter.onTimeout(() -> removeEmitter(userId, emitterId));
        emitter.onError(ex -> removeEmitter(userId, emitterId));

        sendConnectedEvent(userId, emitter);
        return emitter;
    }

    public void emitMissionProgress(MissionProgressEvent event) {
        Map<String, SseEmitter> userEmitters = emittersByUser.get(event.userId());
        if (userEmitters == null || userEmitters.isEmpty()) {
            return;
        }

        String eventType = event.eventType().name();
        List<String> disconnectedEmitterIds = new ArrayList<>();

        userEmitters.forEach((emitterId, emitter) -> {
            meterRegistry.counter(DELIVERY_METRIC, "event_type", eventType, "status", "attempt").increment();
            boolean sent = sendEvent(emitter, "mission-progress", event.eventId(), event);
            if (!sent) {
                meterRegistry.counter(DELIVERY_METRIC, "event_type", eventType, "status", "failure").increment();
                disconnectedEmitterIds.add(emitterId);
                return;
            }
            meterRegistry.counter(DELIVERY_METRIC, "event_type", eventType, "status", "success").increment();
            recordDeliveryLatency(event, eventType);
        });

        disconnectedEmitterIds.forEach(emitterId -> removeEmitter(event.userId(), emitterId));
    }

    @Scheduled(fixedDelay = HEARTBEAT_INTERVAL_MS)
    public void sendHeartbeat() {
        emittersByUser.forEach((userId, emitters) -> {
            List<String> disconnectedEmitterIds = new ArrayList<>();
            emitters.forEach((emitterId, emitter) -> {
                boolean sent = sendEvent(emitter, "ping", null, "keepalive");
                if (!sent) {
                    disconnectedEmitterIds.add(emitterId);
                }
            });
            disconnectedEmitterIds.forEach(emitterId -> removeEmitter(userId, emitterId));
        });
    }

    private void sendConnectedEvent(Long userId, SseEmitter emitter) {
        Map<String, Object> payload = Map.of(
                "status", "subscribed",
                "userId", userId,
                "subscribedAt", LocalDateTime.now()
        );
        boolean sent = sendEvent(emitter, "connected", null, payload);
        if (!sent) {
            emitter.complete();
        }
    }

    private boolean sendEvent(SseEmitter emitter, String eventName, String eventId, Object payload) {
        try {
            SseEmitter.SseEventBuilder builder = SseEmitter.event()
                    .name(eventName)
                    .data(payload);

            if (eventId != null) {
                builder.id(eventId);
            }

            emitter.send(builder);
            return true;
        } catch (IOException | IllegalStateException e) {
            return false;
        }
    }

    private void recordDeliveryLatency(MissionProgressEvent event, String eventType) {
        if (event.occurredAt() == null) {
            return;
        }
        Duration latency = Duration.between(event.occurredAt(), LocalDateTime.now());
        if (latency.isNegative()) {
            return;
        }
        Timer.builder(DELIVERY_LATENCY_METRIC)
            .tag("event_type", eventType)
            .register(meterRegistry)
            .record(latency);
    }

    private void removeEmitter(Long userId, String emitterId) {
        Map<String, SseEmitter> emitters = emittersByUser.get(userId);
        if (emitters == null) {
            return;
        }
        SseEmitter removed = emitters.remove(emitterId);
        if (removed != null) {
            activeEmitterCount.decrementAndGet();
        }
        if (emitters.isEmpty()) {
            emittersByUser.remove(userId);
        }
    }
}
