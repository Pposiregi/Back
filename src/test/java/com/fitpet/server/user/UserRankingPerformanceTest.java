package com.fitpet.server.user;

import com.fitpet.server.user.application.dto.GenderRankingResult;
import com.fitpet.server.user.application.dto.RankingResult;
import com.fitpet.server.user.application.service.UserService;
import com.fitpet.server.user.domain.entity.Gender;
import com.fitpet.server.user.domain.entity.RegistrationStatus;
import com.fitpet.server.user.domain.entity.User;
import com.fitpet.server.user.infra.jpa.UserJpaRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StopWatch;

@SpringBootTest
@ActiveProfiles("local")
class UserRankingPerformanceTest {

    private static final int TARGET_USER_COUNT = 0;      // 생성할 전체 유저 수
    private static final int RANKING_CALL_COUNT = 100;   // 랭킹 API 반복 호출 횟수

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private UserService userService;

    @Test
    void rankingPerformance() {
        System.out.println("===== 성능 측정용 수동 실행 테스트 시작 =====");

        // 더미 유저가 충분히 없으면 채워 넣기
//        prepareUsers();

        // 아무 유저나 하나 골라서 내 순위 계산용으로 사용
        Long targetUserId = userJpaRepository
            .findTopRankers(PageRequest.of(0, 1))
            .get(0)
            .getId();

        StopWatch stopWatch = new StopWatch("user-ranking");

        // 전체 랭킹 성능 측정
        stopWatch.start("getDailyStepRanking");
        for (int i = 0; i < RANKING_CALL_COUNT; i++) {
            RankingResult result = userService.getDailyStepRanking(targetUserId, 10);
        }
        stopWatch.stop();

        // 성별 랭킹 성능 측정
        stopWatch.start("getGenderDailyStepRanking");
        for (int i = 0; i < RANKING_CALL_COUNT; i++) {
            GenderRankingResult result = userService.getGenderDailyStepRanking(Gender.female, 10);
        }
        stopWatch.stop();

        System.out.println();
        System.out.println(stopWatch.prettyPrint());

        long totalDaily = stopWatch.getTaskInfo()[0].getTimeMillis();
        long totalGender = stopWatch.getTaskInfo()[1].getTimeMillis();

        System.out.println("전체 랭킹 총 소요 시간(ms): " + totalDaily);
        System.out.println("전체 랭킹 평균 호출 시간(ms): " + (totalDaily / (double) RANKING_CALL_COUNT));

        System.out.println("성별 랭킹 총 소요 시간(ms): " + totalGender);
        System.out.println("성별 랭킹 평균 호출 시간(ms): " + (totalGender / (double) RANKING_CALL_COUNT));

        System.out.println("===== 성능 측정용 수동 실행 테스트 종료 =====");
    }

    private void prepareUsers() {
        long current = userJpaRepository.count();
        if (current >= TARGET_USER_COUNT) {
            // 이미 TARGET_USER_COUNT 이상이면 아무 것도 안 함
            System.out.println("현재 유저 수: " + current + " -> 더미 유저 생성 스킵");
            return;
        }

        System.out.println("현재 유저 수: " + current + " / 목표: " + TARGET_USER_COUNT + " -> 더미 유저 생성 시작");

        Random random = new Random();
        List<User> batch = new ArrayList<>();

        for (int i = (int) current; i < TARGET_USER_COUNT; i++) {
            User user = User.builder()
                .email("dummy" + i + "@test.com")
                .password("encoded-password") // 실제 인코딩은 필요 없으니 더미 값
                .nickname("user" + i)
                .age(20 + random.nextInt(30))
                .gender(random.nextBoolean() ? Gender.male : Gender.female)
                .dailyStepCount(random.nextInt(20_000)) // 0 ~ 19999
                .registrationStatus(RegistrationStatus.COMPLETE)
                .build();

            batch.add(user);

            if (batch.size() == 1000) { // 1000개씩 배치 저장
                userJpaRepository.saveAll(batch);
                batch.clear();
                System.out.println("더미 유저 1000명 저장...");
            }
        }

        if (!batch.isEmpty()) {
            userJpaRepository.saveAll(batch);
            System.out.println("마지막 배치 " + batch.size() + "명 저장...");
        }

        System.out.println("더미 유저 생성 완료. 총 유저 수: " + userJpaRepository.count());
    }

}