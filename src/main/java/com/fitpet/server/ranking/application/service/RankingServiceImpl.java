package com.fitpet.server.ranking.application.service;

import com.fitpet.server.ranking.application.event.UserCacheRefreshEvent;
import com.fitpet.server.ranking.domain.entity.Ranking;
import com.fitpet.server.ranking.domain.repository.RankingRepository;
import com.fitpet.server.ranking.presentation.dto.RankingResponse;
import com.fitpet.server.user.domain.entity.User;
import com.fitpet.server.user.domain.repository.UserRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingServiceImpl implements RankingService {

    private final RankingRepository rankingRepository;
    private final UserRepository userRepository;

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<Long> updateRankingScript;

    private final ApplicationEventPublisher eventPublisher;

    private static final String USER_PROFILE_KEY = "user:profiles";
    private static final long MAX_TIMESTAMP = 9_999_999_999L;
    private static final double TIME_WEIGHT_DIVIDER = 100_000_000_000.0;
    private static final int TOP_RANK_LIMIT = 10;

    @Override
    @Transactional
    public void updateScore(Long userId, int steps) {
        log.debug("[RankingService] 점수 업데이트 요청: userId={}, steps={}", userId, steps);

        LocalDate now = LocalDate.now();
        String redisKey = getRankingKey(now);
        String dirtyKey = getModifiedUsersKey(now);
        String ttlInSeconds = "259200"; // 3일

        double redisScore = calculateTimeWeightedScore((double) steps, System.currentTimeMillis() / 1000);

        redisTemplate.execute(updateRankingScript,
                List.of(redisKey, dirtyKey),
                String.valueOf(userId), String.valueOf(redisScore), ttlInSeconds
        );

        log.info("[RankingService] 실시간 랭킹 기록 완료: userId={}, steps={}", userId, steps);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RankingResponse> getTop10() {
        return fetchTopRankings(LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public RankingResponse getMyRank(Long userId) {
        LocalDate now = LocalDate.now();
        String redisKey = getRankingKey(now);
        String userIdStr = String.valueOf(userId);

        Long rankIndex = redisTemplate.opsForZSet().reverseRank(redisKey, userIdStr);
        Double redisScore = redisTemplate.opsForZSet().score(redisKey, userIdStr);

        int finalRank = (rankIndex == null) ? calculateDefaultRank(redisKey) : rankIndex.intValue() + 1;
        long finalScore = (redisScore == null) ? 0L : (long) Math.floor(redisScore);

        Map<Long, String> nicknameMap = getNicknameMap(Collections.singletonList(userId));

        return RankingResponse.of(userId, nicknameMap.get(userId), finalRank, finalScore);
    }

    private Map<Long, String> getNicknameMap(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<String> userIdsStr = userIds.stream().map(String::valueOf).toList();
        List<Object> cachedNames = redisTemplate.opsForHash().multiGet(USER_PROFILE_KEY, new ArrayList<>(userIdsStr));

        Map<Long, String> profileMap = new HashMap<>();
        List<Long> missIds = new ArrayList<>();

        for (int i = 0; i < userIds.size(); i++) {
            Long userId = userIds.get(i);
            String nickname = (String) cachedNames.get(i);

            if (nickname == null) {
                missIds.add(userId);
            } else {
                profileMap.put(userId, nickname);
            }
        }

        if (!missIds.isEmpty()) {
            List<User> missingUsers = userRepository.findAllById(missIds);
            missingUsers.forEach(u -> profileMap.put(u.getId(), u.getNickname()));
            eventPublisher.publishEvent(new UserCacheRefreshEvent(missIds));
        }

        return profileMap;
    }

    private List<RankingResponse> fetchTopRankings(LocalDate now) {
        String redisKey = getRankingKey(now);
        Set<ZSetOperations.TypedTuple<String>> tuples =
                redisTemplate.opsForZSet().reverseRangeWithScores(redisKey, 0, TOP_RANK_LIMIT - 1);

        if (tuples == null || tuples.isEmpty()) {
            return recoverRedisFromDatabase(now);
        }

        List<Long> userIds = tuples.stream()
                .map(t -> Long.parseLong(Objects.requireNonNull(t.getValue())))
                .collect(Collectors.toList());
        Map<Long, String> nicknameMap = getNicknameMap(userIds);

        return convertToResponseList(tuples, nicknameMap);
    }

    private List<RankingResponse> recoverRedisFromDatabase(LocalDate now) {
        String dateKey = now.toString();
        log.warn("[RankingService] 캐시 미스 - DB 데이터 복구 시도: {}", dateKey);

        List<Ranking> rankings = rankingRepository.findTop20ByDateKeyOrderByScoreDesc(dateKey);

        if (rankings.isEmpty()) {
            log.info("[RankingService] DB에도 데이터가 없어 빈 결과를 반환합니다.");
            return Collections.emptyList();
        }

        for (Ranking r : rankings) {
            long ts = (r.getUpdatedAt() != null)
                    ? r.getUpdatedAt().atZone(ZoneId.systemDefault()).toEpochSecond()
                    : System.currentTimeMillis() / 1000;

            redisTemplate.opsForZSet().add(getRankingKey(now),
                    String.valueOf(r.getUser().getId()),
                    calculateTimeWeightedScore(r.getScore(), ts));
        }

        List<Ranking> topRankings = rankings.stream()
                .sorted(Comparator.comparingDouble(Ranking::getScore).reversed())
                .limit(TOP_RANK_LIMIT)
                .toList();

        List<Long> userIds = topRankings.stream().map(r -> r.getUser().getId()).toList();
        Map<Long, String> nicknameMap = getNicknameMap(userIds);

        List<RankingResponse> responses = new ArrayList<>();
        int rank = 1;
        for (Ranking r : topRankings) {
            responses.add(RankingResponse.of(
                    r.getUser().getId(),
                    nicknameMap.get(r.getUser().getId()),
                    rank++,
                    (long) Math.floor(r.getScore())));
        }

        return responses;
    }

    private List<RankingResponse> convertToResponseList(Set<ZSetOperations.TypedTuple<String>> tuples,
                                                        Map<Long, String> nicknameMap) {
        List<RankingResponse> result = new ArrayList<>();
        int rank = 1;
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            Long userId = Long.parseLong(Objects.requireNonNull(tuple.getValue()));
            result.add(RankingResponse.of(userId, nicknameMap.get(userId), rank++,
                    (long) Math.floor(Objects.requireNonNull(tuple.getScore()))));
        }
        return result;
    }

    private String getRankingKey(LocalDate date) {
        return "ranking:daily:" + date.toString();
    }
    
    private String getModifiedUsersKey(LocalDate date) {
        return "ranking:dirty:" + date.toString();
    }

    private double calculateTimeWeightedScore(double s, long ts) {
        return s + (double) (MAX_TIMESTAMP - ts) / TIME_WEIGHT_DIVIDER;
    }

    private int calculateDefaultRank(String k) {
        Long s = redisTemplate.opsForZSet().size(k);
        return (s != null ? s.intValue() : 0) + 1;
    }
}