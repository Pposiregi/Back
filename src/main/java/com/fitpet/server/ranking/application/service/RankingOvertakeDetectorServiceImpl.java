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

/**
 * 랭킹 추월 감지 구현체.
 *
 * <h2>추월 감지 알고리즘</h2>
 * <pre>
 * ZSet (ZREVRANGE 기준, 0-indexed):
 *   업데이트 전  : ... [rank 2] [rank 3] [rank 4] [rank 5 = user A] ...
 *   업데이트 후  : ... [rank 2 = user A] [rank 3] [rank 4] [rank 5] ...
 *
 *   previousRank = 4 (0-indexed, 업데이트 전)
 *   newRank      = 1 (0-indexed, 업데이트 후)
 *   추월 당한 사용자 위치 (업데이트 후 기준): newRank+1 ~ previousRank
 * </pre>
 *
 * <h2>동시성 고려</h2>
 * <p>복수의 사용자가 동시에 점수를 업데이트할 때 순위 스냅샷이 미세하게 달라질 수 있다.
 * 알림 특성상 완벽한 정합성보다 low-latency + best-effort 가 더 적합하므로
 * 락 없이 처리한다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RankingOvertakeDetectorServiceImpl implements RankingOvertakeDetectorService {

    private static final String RANKING_KEY_PREFIX = "ranking:daily:";

    private final StringRedisTemplate redisTemplate;
    private final RankingOvertakeOutboxRepository outboxRepository;
    private final OvertakeNotificationRateLimiter rateLimiter;

    /**
     * Spring 이벤트를 비동기로 수신한다.
     *
     * <p>{@code @Async} 덕분에 랭킹 업데이트 요청 스레드를 블록하지 않는다.
     * 별도 스레드풀에서 실행되며, {@code @Transactional}로 아웃박스 저장을 보호한다.</p>
     */
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

        // 랭킹에 처음 진입한 경우 추월한 사람이 없음
        if (previousRank == null) {
            log.info("[OvertakeDetector] previousRank null → 조기 종료 (처음 진입)");
            return;
        }

        String allRankingKey = RANKING_KEY_PREFIX + event.getDate();
        String userIdStr = String.valueOf(event.getUserId());

        Long newRank = redisTemplate.opsForZSet().reverseRank(allRankingKey, userIdStr);

        // 순위가 오르지 않은 경우 (같거나 낮아졌으면 추월 없음)
        if (newRank == null || newRank >= previousRank) {
            return;
        }

        // 업데이트 후 기준으로 newRank+1 ~ previousRank 위치에 있는 사용자들이 추월 당한 사람들
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

            // 자기 자신은 제외 (방어 코드)
            if (overtakenUserId.equals(event.getUserId())) {
                continue;
            }

            // Rate limit 체크: 1시간에 1번만 알림 발송
            if (!rateLimiter.tryAcquire(overtakenUserId)) {
                log.debug("[OvertakeDetector] Rate limit 초과 - userId={}", overtakenUserId);
                continue;
            }

            outboxRepository.save(
                    RankingOvertakeOutbox.builder()
                            .overtakenUserId(overtakenUserId)
                            .overtakingUserId(event.getUserId())
                            .dateKey(event.getDate().toString())
                            .overtakerCount(1) // 단일 이벤트당 1명 추월 (향후 집계 확장 가능)
                            .build()
            );
            savedCount++;
        }

        if (savedCount > 0) {
            log.info("[OvertakeDetector] 아웃박스 저장 완료: {} 건", savedCount);
        }
    }
}

