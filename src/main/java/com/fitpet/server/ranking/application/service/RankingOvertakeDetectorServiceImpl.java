package com.fitpet.server.ranking.application.service;

import com.fitpet.server.ranking.application.event.RankingScoreUpdatedEvent;
import com.fitpet.server.ranking.domain.entity.RankingOvertakeOutbox;
import com.fitpet.server.ranking.domain.repository.RankingOvertakeOutboxRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingOvertakeDetectorServiceImpl implements RankingOvertakeDetectorService {

    private static final String RANKING_KEY_PREFIX = "ranking:daily:";

    private final StringRedisTemplate redisTemplate;
    private final RankingOvertakeOutboxRepository outboxRepository;
    private final OvertakeNotificationRateLimiter rateLimiter;

    @Async
    @EventListener
    @Transactional
    @Override
    public void onRankingScoreUpdated(RankingScoreUpdatedEvent event) {
        try {
            handleOvertake(event);
        } catch (Exception e) {
            log.error("[OvertakeDetector] 처리 중 예외 발생: userId={}", event.getUserId(), e);
        }
    }

    private void handleOvertake(RankingScoreUpdatedEvent event) {
        log.info("[OvertakeDetector] 이벤트 수신: userId={}, previousRank={}, date={}",
                event.getUserId(), event.getPreviousRank(), event.getDate());

        Long previousRank = event.getPreviousRank();

        if (previousRank == null) {
            log.info("[OvertakeDetector] previousRank null → 조기 종료 (처음 진입)");
            return;
        }

        String allRankingKey = RANKING_KEY_PREFIX + event.getDate();
        String userIdStr = String.valueOf(event.getUserId());

        Long newRank = redisTemplate.opsForZSet().reverseRank(allRankingKey, userIdStr);

        if (newRank == null || newRank >= previousRank) {
            return;
        }

        Set<String> overtakenUserIds = redisTemplate.opsForZSet()
                .reverseRange(allRankingKey, newRank + 1, previousRank);

        if (overtakenUserIds == null || overtakenUserIds.isEmpty()) {
            return;
        }

        log.info("[OvertakeDetector] 추월 감지: overtakingUserId={}, newRank={}, previousRank={}, overtakenCount={}",
                event.getUserId(), newRank + 1, previousRank + 1, overtakenUserIds.size());

        int savedCount = 0;
        for (String overtakenIdStr : overtakenUserIds) {
            Long overtakenUserId = Long.parseLong(overtakenIdStr);

            if (overtakenUserId.equals(event.getUserId())) {
                continue;
            }

            if (!rateLimiter.tryAcquire(overtakenUserId)) {
                log.debug("[OvertakeDetector] Rate limit 초과 - userId={}", overtakenUserId);
                continue;
            }

            try {
                outboxRepository.save(
                        RankingOvertakeOutbox.builder()
                                .overtakenUserId(overtakenUserId)
                                .overtakingUserId(event.getUserId())
                                .dateKey(event.getDate().toString())
                                .overtakerCount(1)
                                .build()
                );
            } catch (Exception e) {
                rateLimiter.release(overtakenUserId);
                log.error("[OvertakeDetector] 아웃박스 저장 실패, rate-limit 키 반환: userId={}", overtakenUserId, e);
                continue;
            }
            savedCount++;
        }

        if (savedCount > 0) {
            log.info("[OvertakeDetector] 아웃박스 저장 완료: {} 건", savedCount);
        }
    }
}
