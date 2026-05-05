package com.fitpet.server.ranking.infra.scheduler;

import com.fitpet.server.ranking.domain.entity.RankingOvertakeOutbox;
import com.fitpet.server.ranking.domain.repository.RankingOvertakeOutboxRepository;
import com.fitpet.server.ranking.infra.messaging.RankingOvertakePublisher;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OvertakeOutboxPollerScheduler {

    private static final int BATCH_SIZE = 100;
    private static final int MAX_RETRY = 3;

    private final RankingOvertakeOutboxRepository outboxRepository;
    private final RankingOvertakePublisher publisher;

    @Scheduled(fixedDelay = 30_000)
    public void pollAndPublish() {
        List<RankingOvertakeOutbox> claimed = outboxRepository.claimBatch(BATCH_SIZE);
        if (claimed.isEmpty()) {
            return;
        }

        log.info("[OvertakeOutboxPoller] {}건 발행 시작", claimed.size());

        int successCount = 0;
        int failCount = 0;

        for (RankingOvertakeOutbox outbox : claimed) {
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
                } else {
                    outbox.markPending();
                }
                outboxRepository.save(outbox);
                failCount++;
            }
        }

        log.info("[OvertakeOutboxPoller] 완료 – 성공: {}건, 실패: {}건", successCount, failCount);
    }
}
