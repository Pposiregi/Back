package com.fitpet.server.user;

import com.fitpet.server.user.infra.jpa.UserJpaRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StopWatch;

@SpringBootTest
@ActiveProfiles("local")
class UserNicknameIndexPerformanceTest {

    private static final int LOOKUP_ITERATION = 100;   // 닉네임 조회 반복 횟수

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Test
    void measureNicknameLookupPerformance() {
        System.out.println("===== 닉네임 인덱스 성능 측정 시작 =====");

        String existingNickname = userJpaRepository.findAll(PageRequest.of(0, 1))
            .getContent()
            .get(0)
            .getNickname();

        StopWatch stopWatch = new StopWatch("nickname-index");

        // 실제 존재하는 닉네임으로 existsByNickname 호출
        stopWatch.start("existsByNickname(existing)");
        for (int i = 0; i < LOOKUP_ITERATION; i++) {
            userJpaRepository.existsByNickname(existingNickname);
        }
        stopWatch.stop();

        // 무작위 닉네임으로 미존재 조회 수행
        stopWatch.start("existsByNickname(random)");
        for (int i = 0; i < LOOKUP_ITERATION; i++) {
            userJpaRepository.existsByNickname("missing_" + UUID.randomUUID());
        }
        stopWatch.stop();

        // 닉네임 중복 체크 시 자주 사용하는 existsByNicknameAndIdNot 도 측정
        Long existingUserId = userJpaRepository.findAll(PageRequest.of(0, 1))
            .getContent()
            .get(0)
            .getId();

        stopWatch.start("existsByNicknameAndIdNot");
        for (int i = 0; i < LOOKUP_ITERATION; i++) {
            userJpaRepository.existsByNicknameAndIdNot(existingNickname, existingUserId + i + 1);
        }
        stopWatch.stop();

        System.out.println(stopWatch.prettyPrint());
        System.out.println("===== 닉네임 인덱스 성능 측정 종료 =====");
    }
}
