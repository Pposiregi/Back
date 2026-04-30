package com.fitpet.server.ranking.application.service;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 랭킹 추월 알림 Rate Limiter.
 *
 * <h2>100명이 한꺼번에 추월하는 문제</h2>
 * <p>사용자 A가 걸음수를 크게 올려 100명을 한 번에 추월하면, 100개의 아웃박스 이벤트가
 * 생성될 수 있다. 각 피추월자(overtaken user)에게 알림이 1시간에 최대 1회만 발송되도록
 * Redis SETNX + TTL 방식으로 제한한다.</p>
 *
 * <p>Redis 키: {@code notification:ratelimit:overtake:{overtakenUserId}}<br>
 * TTL: 1시간</p>
 */
@Component
@RequiredArgsConstructor
public class OvertakeNotificationRateLimiter {

    private final StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "notification:ratelimit:overtake:";
    private static final Duration TTL = Duration.ofHours(1);

    /**
     * 알림 발송 슬롯을 획득한다.
     *
     * @param overtakenUserId 알림을 받을 사용자
     * @return 슬롯 획득 성공(발송 허용)이면 {@code true},
     *         이미 해당 시간대에 알림이 발송됐으면 {@code false}
     */
    public boolean tryAcquire(Long overtakenUserId) {
        String key = KEY_PREFIX + overtakenUserId;
        // SETNX : key가 없을 때만 세팅 → 원자적으로 중복 차단
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, "1", TTL);
        return Boolean.TRUE.equals(acquired);
    }

    /**
     * 획득한 슬롯을 반환한다 (보상 트랜잭션용).
     *
     * <p>아웃박스 저장 실패 시 rate-limit 키를 삭제해 다음 이벤트가
     * 차단되지 않도록 복원한다.</p>
     *
     * @param overtakenUserId 알림을 받을 사용자
     */
    public void release(Long overtakenUserId) {
        redisTemplate.delete(KEY_PREFIX + overtakenUserId);
    }
}
