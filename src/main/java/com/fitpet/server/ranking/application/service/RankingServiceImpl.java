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

    private String getCurrentRankingKey() {
        return "ranking:daily:" + LocalDate.now().toString();
    }

    private String getCurrentDateKey() {
        return LocalDate.now().toString();
    }

    // 동점자 처리: 선착순
    @Override
    @Transactional
    public void updateScore(Long userId, double distance) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String dateKey = getCurrentDateKey();
        String redisKey = getCurrentRankingKey();

        // 디비 저장
        Ranking ranking = rankingRepository.findByUserAndDateKey(user, dateKey)
                .orElseGet(() -> Ranking.builder()
                        .user(user)
                        .score(0)
                        .dateKey(dateKey)
                        .build());

        double realScore = ranking.getScore() + distance;
        ranking.updateScore(realScore);
        rankingRepository.save(ranking);

        // 걸음수 뒤에 분별용 실수 더하기(시간이 오래될수록 숫자가 커서 순위가 밀림)
        long currentTimestamp = System.currentTimeMillis() / 1000;
        double timeWeight = (double) (MAX_TIMESTAMP - currentTimestamp) / 100_000_000_000L;

        double redisScore = realScore + timeWeight;

        redisTemplate.opsForZSet().add(redisKey, String.valueOf(userId), redisScore);

        log.info("Rank Update - User: {}, Real: {}, Redis: {}", userId, realScore, redisScore);
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
            Long totalParticipants = redisTemplate.opsForZSet().size(redisKey);

            int myDefaultRank = (totalParticipants != null ? totalParticipants.intValue() : 0) + 1;

            return RankingResponse.builder()
                    .rank(myDefaultRank)
                    .userId(userId)
                    .score(0L)
                    .build();
        }

        // 기록이 있는 경우
        long realScore = (long) Math.floor(redisScore);

        return RankingResponse.builder()
                .rank(rankIndex.intValue() + 1)
                .userId(userId)
                .score(realScore)
                .build();
    }

    private List<RankingResponse> refreshRankingFromDb() {
        log.warn("Recovering Redis from DB...");
        List<Ranking> rankings = rankingRepository.findAllByDateKey(getCurrentDateKey());

        for (Ranking r : rankings) {
            updateScore(r.getUser().getId(), 0);
        }
        return getTop10();
    }
}