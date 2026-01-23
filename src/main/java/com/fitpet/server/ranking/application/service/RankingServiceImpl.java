package com.fitpet.server.ranking.application.service;

import com.fitpet.server.ranking.domain.entity.Ranking;
import com.fitpet.server.ranking.domain.repository.RankingRepository;
import com.fitpet.server.ranking.presentation.dto.RankingResponse;
import com.fitpet.server.user.domain.entity.User;
import com.fitpet.server.user.domain.repository.UserRepository;
import java.time.LocalDate;
import java.time.ZoneId;
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

    private static final long MAX_TIMESTAMP = 9_999_999_999L;
    private static final double TIME_WEIGHT_DIVIDER = 100_000_000_000.0;

    @Override
    @Transactional
    public void updateScore(Long userId, int steps) {
        User user = getUser(userId);
        String dateKey = getCurrentDateKey();
        String redisKey = getCurrentRankingKey();

        Ranking ranking = getOrCreateRanking(user, dateKey);
        double finalRealScore = updateDbRanking(ranking, steps);

        long currentTimestamp = System.currentTimeMillis() / 1000;
        double redisScore = calculateTimeWeightedScore(finalRealScore, currentTimestamp);

        redisTemplate.opsForZSet().add(redisKey, String.valueOf(userId), redisScore);

        log.info("Rank Update - User: {}, Score: {}", userId, finalRealScore);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RankingResponse> getTop10() {
        String redisKey = getCurrentRankingKey();

        Set<ZSetOperations.TypedTuple<String>> tuples =
                redisTemplate.opsForZSet().reverseRangeWithScores(redisKey, 0, 9);

        if (tuples == null || tuples.isEmpty()) {
            return refreshRankingFromDb();
        }

        List<RankingResponse> result = convertToResponseList(tuples);

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public RankingResponse getMyRank(Long userId) {
        String redisKey = getCurrentRankingKey();
        String userIdStr = String.valueOf(userId);

        Long rankIndex = redisTemplate.opsForZSet().reverseRank(redisKey, userIdStr);
        Double redisScore = redisTemplate.opsForZSet().score(redisKey, userIdStr);

        if (rankIndex == null || redisScore == null) {
            Long totalParticipants = redisTemplate.opsForZSet().size(redisKey);
            int myDefaultRank = (totalParticipants != null ? totalParticipants.intValue() : 0) + 1;

            return RankingResponse.builder()
                    .rank(myDefaultRank)
                    .userId(userId)
                    .score(0L)
                    .build();
        }

        long realScore = (long) Math.floor(redisScore);

        return RankingResponse.builder()
                .rank(rankIndex.intValue() + 1)
                .userId(userId)
                .score(realScore)
                .build();
    }

    private List<RankingResponse> refreshRankingFromDb() {
        log.warn("Redis 유실 감지: DB의 원본 시각(updatedAt)을 기준으로 랭킹을 복구합니다.");

        List<Ranking> rankings = rankingRepository.findAllByDateKey(getCurrentDateKey());
        String redisKey = getCurrentRankingKey();

        for (Ranking r : rankings) {
            // ranking table의 updated_at을 이용해 순위 재측정
            long originalTimestamp = (r.getUpdatedAt() != null)
                    ? r.getUpdatedAt().atZone(ZoneId.systemDefault()).toEpochSecond()
                    : System.currentTimeMillis() / 1000; // null일 경우 현재시간 사용

            double redisScore = calculateTimeWeightedScore(r.getScore(), originalTimestamp);
            redisTemplate.opsForZSet().add(redisKey, String.valueOf(r.getUser().getId()), redisScore);
        }

        return getTop10();
    }

    private double calculateTimeWeightedScore(double realScore, long timestamp) {
        double timeWeight = (double) (MAX_TIMESTAMP - timestamp) / TIME_WEIGHT_DIVIDER;
        return realScore + timeWeight;
    }

    private Ranking getOrCreateRanking(User user, String dateKey) {
        return rankingRepository.findByUserAndDateKey(user, dateKey)
                .orElseGet(() -> Ranking.builder()
                        .user(user)
                        .score(0)
                        .dateKey(dateKey)
                        .build());
    }

    private double updateDbRanking(Ranking ranking, int steps) {
        double newTotalScore = ranking.getScore() + steps;
        ranking.updateScore(newTotalScore);
        rankingRepository.save(ranking);
        return newTotalScore;
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private String getCurrentRankingKey() {
        return "ranking:daily:" + LocalDate.now().toString();
    }

    private String getCurrentDateKey() {
        return LocalDate.now().toString();
    }

    private List<RankingResponse> convertToResponseList(Set<ZSetOperations.TypedTuple<String>> tuples) {
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
}