package com.fitpet.server.ranking.infra.scheduler;

import com.fitpet.server.ranking.domain.entity.RankingOvertakeOutbox;
import com.fitpet.server.ranking.domain.repository.RankingOvertakeOutboxRepository;
import com.fitpet.server.ranking.infra.messaging.RankingOvertakePublisher;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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
 * <h2>3단계 발행 프로토콜</h2>
 * <ol>
 *   <li><b>Claim (TX 1)</b> – PENDING 행을 SELECT FOR UPDATE SKIP LOCKED 로 잠근 뒤
 *       status=CLAIMED 로 커밋. 다른 인스턴스가 같은 행을 가져가지 못한다.</li>
 *   <li><b>Publish (TX 없음)</b> – MQ 에 메시지를 발행한다.
 *       이 단계는 DB 트랜잭션 밖에서 수행되므로 "발행 성공 + DB 커밋 실패 → 재발행"
 *       경로가 차단된다. 소비자는 outboxId 기반 멱등 처리로 중복을 방어한다.</li>
 *   <li><b>결과 저장 (TX 2)</b> – 발행 성공 시 PUBLISHED, 실패 시 retryCount 증가 후
 *       PENDING 으로 복원 (다음 폴링 주기에서 재시도). MAX_RETRY 초과 시 FAILED.</li>
 * </ol>
 *
 * <h2>CLAIMED stuck 행 복구</h2>
 * <p>JVM 충돌로 Claim 후 Publish/결과저장 전에 프로세스가 종료되면 행이 CLAIMED 에
 * 고착될 수 있다. 주기적 클린업 잡(별도 구현 예정)에서 일정 시간 이상 CLAIMED인
 * 행을 PENDING 으로 복원한다.</p>
 *
 * <h2>재시도 정책</h2>
 * <ul>
 *   <li>MQ 발행 실패 시 retryCount를 증가시키고 PENDING 복원 → 다음 폴링에서 재시도.</li>
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
     * <p>fixedDelay 사용 → 이전 실행이 완료된 후 30초 대기 (동시 실행 방지).
     * 메서드 자체에 {@code @Transactional} 을 두지 않는다. 트랜잭션 경계는
     * claimBatch / save 내부에서 각각 관리한다.</p>
     */
    @Scheduled(fixedDelay = 30_000)
    public void pollAndPublish() {
        // ── Phase 1: Claim ────────────────────────────────────────────────
        // 별도 TX 에서 PENDING → CLAIMED 커밋. 이 TX 가 끝난 뒤 MQ 발행.
        List<RankingOvertakeOutbox> claimed = outboxRepository.claimBatch(BATCH_SIZE);
        if (claimed.isEmpty()) {
            return;
        }

        log.info("[OvertakeOutboxPoller] {}건 발행 시작", claimed.size());

        int successCount = 0;
        int failCount = 0;

        for (RankingOvertakeOutbox outbox : claimed) {
            // ── Phase 2: Publish (DB TX 밖) ───────────────────────────────
            try {
                publisher.publish(outbox);

                // ── Phase 3a: 발행 성공 → PUBLISHED (새 TX) ───────────────
                outbox.markPublished();
                outboxRepository.save(outbox);
                successCount++;

            } catch (Exception e) {
                log.error("[OvertakeOutboxPoller] 발행 실패: outboxId={}", outbox.getId(), e);

                // ── Phase 3b: 발행 실패 → 재시도 또는 FAILED (새 TX) ──────
                outbox.incrementRetryCount();
                if (outbox.getRetryCount() >= MAX_RETRY) {
                    outbox.markFailed();
                    log.error("[OvertakeOutboxPoller] 최대 재시도 초과 – FAILED 처리: outboxId={}", outbox.getId());
                } else {
                    // PENDING 으로 복원해 다음 폴링 주기에서 재시도
                    outbox.markPending();
                }
                outboxRepository.save(outbox);
                failCount++;
            }
        }

        log.info("[OvertakeOutboxPoller] 완료 – 성공: {}건, 실패: {}건", successCount, failCount);
    }
}
