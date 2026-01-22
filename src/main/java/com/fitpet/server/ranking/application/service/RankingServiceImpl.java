package com.fitpet.server.ranking.application.service;

import com.fitpet.server.ranking.application.dto.RankingDto;
import com.fitpet.server.user.domain.entity.User;
import com.fitpet.server.user.domain.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingServiceImpl implements RankingService {

    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;
    
    private static final String RANKING_KEY = "ranking:weekly";

    @Override
    @Transactional
    public void updateScore(Long userId, double distance) {
        // 1. 사용자 조회 (User가 점수의 주체)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 2. DB 업데이트 (User 엔티티 직접 수정)
        // ⚠️ [중요] User 엔티티에 점수(또는 거리)를 누적하는 메서드가 필요합니다.
        // 예: user.addTotalDistance(distance); 또는 user.updateScore(...);
        // 아래는 예시 로직입니다. 상황에 맞는 User 메서드를 호출하세요.
        /* double currentScore = user.getTotalScore(); // User 엔티티의 Getter
           double newScore = currentScore + distance;
           user.updateScore(newScore); // User 엔티티의 Setter/Update 메서드
        */

        // (임시) User 엔티티의 내부 로직으로 점수가 변경되었다고 가정하고 저장
        userRepository.save(user);

        // 3. Redis ZSet 업데이트 (실시간 랭킹용) ⚡️
        // User 엔티티에서 최신 점수를 가져와서 Redis에 반영
        // (여기서는 distance가 더해진 최종 점수를 넣어야 합니다. 편의상 user.getScore()라고 가정)
        // 만약 User에 getter가 없다면, Redis Increment 기능을 써도 됩니다.

        // 방법 A: Redis에 점수 누적 (Increment) - 가장 간단!
        Double totalScore = redisTemplate.opsForZSet().incrementScore(RANKING_KEY, String.valueOf(userId), distance);

        log.info("[Ranking] User: {}, NewScore: {}", userId, totalScore);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RankingDto> getTop10() {
        // Redis에서 점수가 높은 순(Reverse)으로 0등~9등 조회
        Set<ZSetOperations.TypedTuple<String>> tuples =
                redisTemplate.opsForZSet().reverseRangeWithScores(RANKING_KEY, 0, 9);

        // Redis 데이터가 없으면 DB에서 복구 시도
        if (tuples == null || tuples.isEmpty()) {
            return refreshRankingFromDb();
        }

        List<RankingDto> result = new ArrayList<>();
        int rank = 1;

        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            String userIdStr = tuple.getValue();
            Double score = tuple.getScore();
            if (userIdStr != null) {
                result.add(new RankingDto(rank++, Long.parseLong(userIdStr), score));
            }
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public RankingDto getMyRank(Long userId) {
        String key = String.valueOf(userId);

        // 내 순위 조회 (0부터 시작하므로 +1)
        Long rank = redisTemplate.opsForZSet().reverseRank(RANKING_KEY, key);
        // 내 점수 조회
        Double score = redisTemplate.opsForZSet().score(RANKING_KEY, key);

        if (rank == null) {
            // Redis에 없으면 DB 확인 후 Redis에 넣는 로직이 있을 수도 있음
            return null;
        }

        return new RankingDto(rank.intValue() + 1, userId, score);
    }

    // 🚨 Redis 데이터 유실 시 DB에서 복구하는 메서드
    private List<RankingDto> refreshRankingFromDb() {
        log.warn("[Ranking] Redis 데이터 유실 감지! DB에서 복구합니다.");

        // UserRepositoryAdapter에 있는 'findTopRankers'를 활용하여 상위 랭커들을 가져옵니다.
        // 혹은 전체 유저를 가져와야 한다면 findAll() 등을 사용해야 합니다.
        // 여기서는 캐시 워밍업을 위해 상위 100명 정도만 먼저 복구하거나, 전체를 복구합니다.
        List<User> topUsers = userRepository.findTopRankers(100);

        for (User u : topUsers) {
            // User 엔티티에서 랭킹에 쓰이는 점수 필드를 가져옵니다. (예: getDailyStepCount or getScore)
            // 여기서는 getDailyStepCount()를 예시로 듭니다. 상황에 맞게 변경하세요.
            // double score = u.getDailyStepCount();

            // redisTemplate.opsForZSet().add(RANKING_KEY, String.valueOf(u.getId()), score);
        }

        return getTop10(); // 복구 후 다시 조회
    }
}