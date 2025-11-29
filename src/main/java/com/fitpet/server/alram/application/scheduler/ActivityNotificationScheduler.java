package com.fitpet.server.alram.application.scheduler;

import com.fitpet.server.alram.application.service.AlramBatchService;
import com.fitpet.server.user.domain.entity.User;
import com.fitpet.server.user.domain.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityNotificationScheduler {

    private final UserRepository userRepository;
    private final AlramBatchService alramBatchService;

    private static final int BATCH_SIZE = 500;

    private static final List<String> ENCOURAGEMENT_MESSAGES = List.of(
            "오늘 목표 걸음까지 조금만 더 힘내세요! 🏃",
            "아직 늦지 않았어요! 지금 가벼운 산책 어떠세요?",
            "움직일 시간이에요! 목표 달성이 얼마 남지 않았어요.",
            "FitPet이 응원할게요! 조금만 더 걸어볼까요?"
    );

    private static final List<String> INACTIVE_MESSAGES = List.of(
            "오랜만이에요! FitPet이 기다리고 있어요. 👟",
            "다시 함께 걸어볼까요? 건강한 습관을 되찾아보세요!",
            "회원님을 위한 맞춤 운동 정보가 업데이트되었어요!"
    );

    private final Random random = new Random();

    @Scheduled(cron = "0 0 20 * * ?", zone = "Asia/Seoul") // 매일 20시 정각
    //@Scheduled(cron = "0 * * * * ?", zone = "Asia/Seoul") // Test용 매분 실행
    public void scheduleStepEncouragement() {
        log.info("[스케줄러 시작] 걷기 독려 알림 (목표 50% 미만 사용자)");

        Pageable pageable = PageRequest.of(0, BATCH_SIZE);
        boolean hasNextPage = true;
        int totalSentCount = 0;

        while (hasNextPage) {
            Page<User> userPage = userRepository.findUsersBelowStepTarget(pageable);

            if (!userPage.hasContent()) {
                if (totalSentCount == 0) {
                    log.info("[스케줄러] 걷기 독려 알림 대상자가 없습니다.");
                }
                break;
            }

            List<String> tokens = userPage.getContent().stream()
                    .map(User::getDeviceToken)
                    .collect(Collectors.toList());

            String title = "👟 FitPet 걸음 독려";
            String body = getRandomEncouragementMessage();
            Map<String, String> data = Map.of("screen", "/main");

            alramBatchService.sendMulticastNotification(tokens, title, body, data);

            totalSentCount += tokens.size();

            if (userPage.hasNext()) {
                pageable = userPage.nextPageable();
            } else {
                hasNextPage = false;
            }
        }

        log.info("[스케줄러 종료] 총 {}명에게 걷기 독려 알림 발송 완료", totalSentCount);
    }

    @Scheduled(cron = "0 0 10 * * 1", zone = "Asia/Seoul")
    public void scheduleInactivityNotification() {
        log.info("[스케줄러 시작] 비활성 사용자(7일) 알림");

        final int INACTIVE_DAYS = 7;
        LocalDateTime inactiveSince = LocalDateTime.now().minusDays(INACTIVE_DAYS);

        Pageable pageable = PageRequest.of(0, BATCH_SIZE);
        boolean hasNextPage = true;
        int totalSentCount = 0;

        while (hasNextPage) {
            Page<User> userPage = userRepository.findInactiveUsers(inactiveSince, pageable);

            if (!userPage.hasContent()) {
                if (totalSentCount == 0) {
                    log.info("[스케줄러] 비활성 사용자 알림 대상자가 없습니다.");
                }
                break;
            }

            List<String> tokens = userPage.getContent().stream()
                    .map(User::getDeviceToken)
                    .collect(Collectors.toList());

            String title = "FitPet이 기다리고 있어요! 👋";
            String body = getRandomInactiveMessage();
            Map<String, String> data = Map.of("screen", "/main");

            alramBatchService.sendMulticastNotification(tokens, title, body, data);

            totalSentCount += tokens.size();

            if (userPage.hasNext()) {
                pageable = userPage.nextPageable();
            } else {
                hasNextPage = false;
            }
        }

        log.info("[스케줄러 종료] 총 {}명에게 비활성 알림 발송 완료", totalSentCount);
    }


    private String getRandomEncouragementMessage() {
        return ENCOURAGEMENT_MESSAGES.get(random.nextInt(ENCOURAGEMENT_MESSAGES.size()));
    }

    private String getRandomInactiveMessage() {
        return INACTIVE_MESSAGES.get(random.nextInt(INACTIVE_MESSAGES.size()));
    }
}