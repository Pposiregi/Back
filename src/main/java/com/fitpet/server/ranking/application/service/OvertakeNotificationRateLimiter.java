package com.fitpet.server.ranking.application.service;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OvertakeNotificationRateLimiter {

    private final StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "notification:ratelimit:overtake:";
    private static final Duration TTL = Duration.ofHours(1);

    public boolean tryAcquire(Long overtakenUserId) {
        String key = KEY_PREFIX + overtakenUserId;
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, "1", TTL);
        return Boolean.TRUE.equals(acquired);
    }

    public void release(Long overtakenUserId) {
        redisTemplate.delete(KEY_PREFIX + overtakenUserId);
    }
}
