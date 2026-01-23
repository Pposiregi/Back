package com.fitpet.server.ranking.application.service;

import com.fitpet.server.ranking.domain.entity.Ranking;
import com.fitpet.server.ranking.domain.repository.RankingRepository;
import com.fitpet.server.ranking.presentation.dto.RankingResponse;
import com.fitpet.server.user.domain.entity.User;
import com.fitpet.server.user.domain.repository.UserRepository;
import java.time.LocalDate;
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

    private final RankingRepository rankingRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;

    // 시간 가중치 계산용 상수
    private static final long MAX_TIMESTAMP = 9_999_999_999L;
    private static final double TIME_WEIGHT_DIVIDER = 100_000_000_000.0;

    @Override
    @Transactional
    public void updateScore(Long userId, int steps) {
        User user = getUser(userId);
        String dateKey = getCurrentDateKey();
        String redisKey = getCurrentRankingKey();

        // DB 업데이트
        Ranking ranking = getOrCreateRanking(user, dateKey);
        double finalRealScore = updateDbRanking(ranking, steps);

        // Redis 가중치 점수 계산 및 반영
        double redisScore = calculateTimeWeightedScore(finalRealScore);
        redisTemplate.opsForZSet().add(redisKey, String.valueOf(userId), redisScore);

        log.info("Rank Update - User: {}, RealScore: {}, RedisScore: {}", userId, finalRealScore, redisScore);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RankingResponse> getTop10() {
        String redisKey = getCurrentRankingKey();

        // 내림차순 10개
        Set<ZSetOperations.TypedTuple<String>> tuples =
                redisTemplate.opsForZSet().reverseRangeWithScores(redisKey, 0, 9);

        if (tuples == null || tuples.isEmpty()) {
            return refreshRankingFromDb(); // 복구 로직
        }

        List<RankingResponse> result = new ArrayList<>();
        int rank = 1;

        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            String userIdStr = tuple.getValue();
            Double redisScore = tuple.getScore();

            if (userIdStr != null && redisScore != null) {
                long realScore = (long) Math.floor(redisScore);

                result.add(RankingResponse.builder()
                        .rank(rank++)
                        .userId(Long.parseLong(userIdStr))
                        .score(realScore)
                        .build());
            }
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public RankingResponse getMyRank(Long userId) {
        String redisKey = getCurrentRankingKey();
        String userIdStr = String.valueOf(userId);

        Long rankIndex = redisTemplate.opsForZSet().reverseRank(redisKey, userIdStr);
        Double redisScore = redisTemplate.opsForZSet().score(redisKey, userIdStr);

        // 오늘 기록이 없는 사용자 처리
        if (rankIndex == null || redisScore == null) {
            // 현재 Redis 랭킹(ZSet)에 등록된 총 인원수를 가져옴
            Long totalParticipants = redisTemplate.opsForZSet().size(redisKey);

            // 내 등수 = (현재 참여 인원수) + 1
            int myDefaultRank = (totalParticipants != null ? totalParticipants.intValue() : 0) + 1;

            return RankingResponse.builder()
                    .rank(myDefaultRank)
                    .userId(userId)
                    .score(0L) // 기록이 없으므로 0점 (long 타입) 반환
                    .build();
        }

        // 3. 기록이 있는 경우: 점수 복원 및 정수(long) 형변환
        long realScore = (long) Math.floor(redisScore);

        return RankingResponse.builder()
                .rank(rankIndex.intValue() + 1) // 0-based index를 1-based 순위로 변환
                .userId(userId)
                .score(realScore) // 소수점이 제거된 정수값 반환
                .build();
    }

    private String getCurrentRankingKey() {
        return "ranking:daily:" + LocalDate.now().toString();
    }

    private String getCurrentDateKey() {
        return LocalDate.now().toString();
    }

    private Ranking getOrCreateRanking(User user, String dateKey) {
        // 오늘 날짜의 랭킹 정보를 조회한다.
        Ranking result = rankingRepository.findByUserAndDateKey(user, dateKey)
                .orElseGet(() -> Ranking.builder()
                        .user(user)
                        .score(0)
                        .dateKey(dateKey)
                        .build());

        return result;
    }

    private double updateRedisRanking(Ranking ranking, int steps) {
        double realScore = ranking.getScore() + steps;
        ranking.updateScore(realScore);
        rankingRepository.save(ranking);

        return realScore;
    }

    private User getUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return user;
    }

    private double calculateTimeWeightedScore(double realScore) {
        long currentTimestamp = System.currentTimeMillis() / 1000;
        double timeWeight = (MAX_TIMESTAMP - currentTimestamp) / TIME_WEIGHT_DIVIDER;
        return realScore + timeWeight;
    }

    private double updateDbRanking(Ranking ranking, int steps) {
        double newTotalScore = ranking.getScore() + steps;
        ranking.updateScore(newTotalScore);
        rankingRepository.save(ranking);
        return newTotalScore;
    }

    // Db에서 랭킹 가져와 Redis에 올리기
    private List<RankingResponse> refreshRankingFromDb() {
        log.warn("Recovering Redis from DB...");
        List<Ranking> rankings = rankingRepository.findAllByDateKey(getCurrentDateKey());

        for (Ranking r : rankings) {
            // 복구 시에는 시간 정보가 소실되어서, 복구 시점 기준으로 순서가 정해질 수 있음
            // 정확성을 위해 Ranking 엔티티에 'last_updated_at'이 있다면 그걸 활용 가능
            updateScore(r.getUser().getId(), 0);
        }
        return getTop10();
    }
}