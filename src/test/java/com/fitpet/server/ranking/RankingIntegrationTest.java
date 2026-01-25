// 1. 패키지 선언 누락 해결
package com.fitpet.server.ranking;

// 2. Assertions import 수정 (ClassTypes 대신 일반 Assertions 사용)

import static org.assertj.core.api.Assertions.assertThat;

import com.fitpet.server.ranking.application.service.RankingService;
import com.fitpet.server.ranking.domain.entity.Ranking;
import com.fitpet.server.ranking.domain.repository.RankingRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
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

    @Test
    @DisplayName("스케줄러 동기화 후 DB에 저장이 되고 Dirty 플래그는 삭제되어야 한다")
    void syncToDbTest() {
        // given
        String dateKey = LocalDate.now().toString();
        rankingService.updateScore(1L, 7000);

        // when
        rankingService.syncAllDirtyRanksToDb();

        // then
        //  DB 확인
        List<Ranking> rankings = rankingRepository.findAllByDateKey(dateKey);
        assertThat(rankings).isNotEmpty();
        assertThat(rankings.get(0).getScore()).isEqualTo(7000);

        //  Dirty 플래그 삭제 확인
        Set<String> dirty = redisTemplate.opsForSet().members("ranking:dirty:" + dateKey);
        assertThat(dirty).isNotNull().isEmpty();
    }
}