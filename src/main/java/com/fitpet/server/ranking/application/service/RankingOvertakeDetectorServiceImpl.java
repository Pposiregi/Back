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

        Long newRank = resolveNewRank(event);
        if (newRank == null) {
            return;
        }

        Set<String> overtakenUserIds = getOvertakenUserIds(event, newRank);
        if (overtakenUserIds == null || overtakenUserIds.isEmpty()) {
            return;
        }

        log.info("[OvertakeDetector] 추월 감지: overtakingUserId={}, newRank={}, previousRank={}, overtakenCount={}",
                event.getUserId(), newRank + 1, event.getPreviousRank() + 1, overtakenUserIds.size());

        int savedCount = saveOutboxForEligibleUsers(overtakenUserIds, event);
        if (savedCount > 0) {
            log.info("[OvertakeDetector] 아웃박스 저장 완료: {} 건", savedCount);
        }
    }

    private Long resolveNewRank(RankingScoreUpdatedEvent event) {
        Long previousRank = event.getPreviousRank();
        if (previousRank == null) {
            log.info("[OvertakeDetector] previousRank null → 조기 종료 (처음 진입)");
            return null;
        }

        String allRankingKey = RANKING_KEY_PREFIX + event.getDate();
        Long newRank = redisTemplate.opsForZSet().reverseRank(allRankingKey, String.valueOf(event.getUserId()));

        if (newRank == null || newRank >= previousRank) {
            return null;
        }
        return newRank;
    }

    private Set<String> getOvertakenUserIds(RankingScoreUpdatedEvent event, Long newRank) {
        String allRankingKey = RANKING_KEY_PREFIX + event.getDate();
        return redisTemplate.opsForZSet()
                .reverseRange(allRankingKey, newRank + 1, event.getPreviousRank());
    }

    private int saveOutboxForEligibleUsers(Set<String> overtakenUserIds, RankingScoreUpdatedEvent event) {
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

            if (saveOutbox(overtakenUserId, event)) {
                savedCount++;
            }
        }
        return savedCount;
    }

    private boolean saveOutbox(Long overtakenUserId, RankingScoreUpdatedEvent event) {
        try {
            outboxRepository.save(
                    RankingOvertakeOutbox.builder()
                            .overtakenUserId(overtakenUserId)
                            .overtakingUserId(event.getUserId())
                            .dateKey(event.getDate().toString())
                            .overtakerCount(1)
                            .build()
            );
            return true;
        } catch (Exception e) {
            rateLimiter.release(overtakenUserId);
            log.error("[OvertakeDetector] 아웃박스 저장 실패, rate-limit 키 반환: userId={}", overtakenUserId, e);
            return false;
        }
    }
}
