// 1. 패키지 선언 누락 해결
package com.fitpet.server.ranking;

// 2. Assertions import 수정 (ClassTypes 대신 일반 Assertions 사용)

import static org.assertj.core.api.Assertions.assertThat;

import com.fitpet.server.ranking.application.service.RankingService;
import com.fitpet.server.ranking.domain.repository.RankingRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
class RankingIntegrationTest {

    @Autowired
    private RankingService rankingService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RankingRepository rankingRepository;

    @Test
    @DisplayName("랭킹 업데이트 시 Redis 데이터와 Dirty 플래그가 동시에 생성되어야 한다")
    void redisUpdateTest() {
        // given
        Long userId = 1L;
        int steps = 5000;
        String dateKey = LocalDate.now().toString();

        // when
        rankingService.updateScore(userId, steps);

        // then
        // ZSET 확인
        Double score = redisTemplate.opsForZSet().score("ranking:daily:" + dateKey, String.valueOf(userId));
        assertThat(score).isNotNull();
        assertThat(Math.floor(score)).isEqualTo(steps);

        // Dirty SET 확인
        Boolean isDirty = redisTemplate.opsForSet().isMember("ranking:dirty:" + dateKey, String.valueOf(userId));
        assertThat(isDirty).isTrue();
    }

}