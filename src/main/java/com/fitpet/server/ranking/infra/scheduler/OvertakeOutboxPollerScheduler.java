package com.fitpet.server.ranking.infra.scheduler;

import com.fitpet.server.ranking.domain.entity.RankingOvertakeOutbox;
import com.fitpet.server.ranking.domain.repository.RankingOvertakeOutboxRepository;
import com.fitpet.server.ranking.infra.messaging.RankingOvertakePublisher;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 아웃박스 폴러 스케줄러 (Outbox Poller).
 *
 * <h2>역할</h2>
 * <p>DB에 PENDING 상태로 저장된 아웃박스 이벤트를 30초마다 배치로 읽어
 * RabbitMQ에 발행하고 PUBLISHED 로 상태를 변경한다.</p>
 *
 * <h2>왜 아웃박스 패턴이 필요한가?</h2>
 * <p>랭킹 업데이트(Redis Lua 스크립트)와 MQ 발행은 단일 트랜잭션으로 묶을 수 없다.
 * 서버 재시작·MQ 일시 다운 시 이벤트가 유실되는 것을 방지하기 위해
 * "DB에 먼저 기록 → 폴러가 안정적으로 발행" 흐름을 사용한다.</p>
 *
 * <h2>재시도 정책</h2>
 * <ul>
 *   <li>MQ 발행 실패 시 retryCount를 증가시키고 PENDING 유지 → 다음 폴링에서 재시도.</li>
 *   <li>retryCount ≥ 3 이면 FAILED 처리 → 수동 확인.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OvertakeOutboxPollerScheduler {

    private static final int BATCH_SIZE = 100;
    private static final int MAX_RETRY = 3;

    private final RankingOvertakeOutboxRepository outboxRepository;
    private final RankingOvertakePublisher publisher;

    /**
     * 30초마다 PENDING 이벤트를 최대 100건 발행한다.
     *
     * <p>fixedDelay 사용 → 이전 실행이 완료된 후 30초 대기 (동시 실행 방지).</p>
     */
    @Scheduled(fixedDelay = 30_000)
    @Transactional
    public void pollAndPublish() {
        List<RankingOvertakeOutbox> pending = outboxRepository.findPendingBatch(BATCH_SIZE);
        if (pending.isEmpty()) {
            return;
        }

        log.info("[OvertakeOutboxPoller] {}건 발행 시작", pending.size());

        int successCount = 0;
        int failCount = 0;

        for (RankingOvertakeOutbox outbox : pending) {
            try {
                publisher.publish(outbox);
                outbox.markPublished();
                outboxRepository.save(outbox);
                successCount++;
            } catch (Exception e) {
                log.error("[OvertakeOutboxPoller] 발행 실패: outboxId={}", outbox.getId(), e);
                outbox.incrementRetryCount();
                if (outbox.getRetryCount() >= MAX_RETRY) {
                    outbox.markFailed();
                    log.error("[OvertakeOutboxPoller] 최대 재시도 초과 – FAILED 처리: outboxId={}", outbox.getId());
                }
                outboxRepository.save(outbox);
                failCount++;
            }
        }

        log.info("[OvertakeOutboxPoller] 완료 – 성공: {}건, 실패: {}건", successCount, failCount);
    }
}
