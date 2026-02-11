package com.fitpet.server.ranking.application.service;

import com.fitpet.server.ranking.application.dto.RankingDto;
import com.fitpet.server.ranking.application.event.UserCacheRefreshEvent;
import com.fitpet.server.ranking.domain.entity.Ranking;
import com.fitpet.server.ranking.domain.repository.RankingRepository;
import com.fitpet.server.ranking.domain.type.RankingFilter;
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
import org.springframework.data.domain.PageRequest;
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
    // 성별 캐싱을 위한 Redis Hash Key 추가
    private static final String USER_GENDER_CACHE_KEY = "user:genders";

    private static final long MAX_TIMESTAMP = 9_999_999_999L;
    private static final double TIME_WEIGHT_DIVIDER = 100_000_000_000.0;
    private static final int TOP_RANK_LIMIT = 10;
    private static final String TTL_SECONDS = "259200";

    @Override
    @Transactional
    public void updateScore(Long userId, int steps) {
        LocalDate now = LocalDate.now();
        String dirtyKey = getModifiedUsersKey(now);

        double weightedScore = calculateTimeWeightedScore((double) steps, System.currentTimeMillis() / 1000);
        String userIdStr = String.valueOf(userId);

        // 유저 성별 조회 (Redis Hash -> 없으면 DB Lazy Loading)
        RankingFilter userGender = getUserGender(userId);

        // 전체 랭킹 키와 성별 랭킹 키 생성
        String allRankingKey = getRankingKey(now, RankingFilter.ALL);
        String genderRankingKey = getRankingKey(now, userGender);

        // 전체 랭킹 업데이트
        redisTemplate.execute(updateRankingScript,
                List.of(allRankingKey, dirtyKey),
                userIdStr,
                String.valueOf(weightedScore),
                TTL_SECONDS
        );

        // 성별 랭킹 업데이트
        if (userGender != RankingFilter.ALL) {
            redisTemplate.execute(updateRankingScript,
                    List.of(genderRankingKey, dirtyKey),
                    userIdStr,
                    String.valueOf(weightedScore),
                    TTL_SECONDS
            );
        }

        log.info("[RankingService] 랭킹 업데이트 완료 (Lua): userId={}, score={}, gender={}", userId, weightedScore,
                userGender);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RankingDto> getTop10(RankingFilter filter) {
        return fetchTopRankings(LocalDate.now(), filter);
    }

    @Override
    @Transactional(readOnly = true)
    public RankingDto getMyRank(Long userId, RankingFilter filter) {
        LocalDate now = LocalDate.now();
        // 필터에 맞는 Redis Key 조회
        String redisKey = getRankingKey(now, filter);
        String userIdStr = String.valueOf(userId);

        Long rankIndex = redisTemplate.opsForZSet().reverseRank(redisKey, userIdStr);
        Double redisScore = redisTemplate.opsForZSet().score(redisKey, userIdStr);

        int finalRank = (rankIndex == null) ? calculateDefaultRank(redisKey) : rankIndex.intValue() + 1;
        long finalScore = (redisScore == null) ? 0L : (long) Math.floor(redisScore);

        Map<Long, String> nicknameMap = getNicknameMap(Collections.singletonList(userId));

        return RankingDto.of(userId, nicknameMap.get(userId), finalRank, finalScore);
    }

    private RankingFilter getUserGender(Long userId) {
        String userIdStr = String.valueOf(userId);

        Object cachedGender = redisTemplate.opsForHash().get(USER_GENDER_CACHE_KEY, userIdStr);
        if (cachedGender != null) {
            return RankingFilter.valueOf((String) cachedGender);
        }

        // 캐시 미스 -> DB 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        RankingFilter filter = convertGender(user.getGender());

        redisTemplate.opsForHash().put(USER_GENDER_CACHE_KEY, userIdStr, filter.name());

        return filter;
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

    private List<RankingDto> fetchTopRankings(LocalDate now, RankingFilter filter) {
        String redisKey = getRankingKey(now, filter);
        Set<ZSetOperations.TypedTuple<String>> tuples =
                redisTemplate.opsForZSet().reverseRangeWithScores(redisKey, 0, TOP_RANK_LIMIT - 1);

        if (tuples == null || tuples.isEmpty()) {
            return recoverRedisFromDatabase(now, filter);
        }

        List<Long> userIds = tuples.stream()
                .map(t -> Long.parseLong(Objects.requireNonNull(t.getValue())))
                .collect(Collectors.toList());
        Map<Long, String> nicknameMap = getNicknameMap(userIds);

        return convertToResponseList(tuples, nicknameMap);
    }

    private List<RankingDto> recoverRedisFromDatabase(LocalDate now, RankingFilter filter) {
        String dateKey = now.toString();
        log.warn("[RankingService] 캐시 미스 - DB 데이터 복구 시도: {}", dateKey);

        List<Ranking> rankings = rankingRepository.findTopRankings(dateKey, PageRequest.of(0, 100)); // 복구 시 넉넉하게 조회

        if (rankings.isEmpty()) {
            log.info("[RankingService] DB에도 데이터가 없어 빈 결과를 반환합니다.");
            return Collections.emptyList();
        }

        String allKey = getRankingKey(now, RankingFilter.ALL);
        String maleKey = getRankingKey(now, RankingFilter.MALE);
        String femaleKey = getRankingKey(now, RankingFilter.FEMALE);

        for (Ranking r : rankings) {
            long ts = (r.getUpdatedAt() != null)
                    ? r.getUpdatedAt().atZone(ZoneId.systemDefault()).toEpochSecond()
                    : System.currentTimeMillis() / 1000;

            double weightedScore = calculateTimeWeightedScore(r.getScore(), ts);
            String userIdStr = String.valueOf(r.getUser().getId());
            RankingFilter gender = convertGender(r.getUser().getGender());

            // 전체 랭킹 복구
            redisTemplate.opsForZSet().add(allKey, userIdStr, weightedScore);

            // 성별 랭킹 복구 (성별에 맞춰 해당 키에 적재)
            if (gender == RankingFilter.MALE) {
                redisTemplate.opsForZSet().add(maleKey, userIdStr, weightedScore);
            } else if (gender == RankingFilter.FEMALE) {
                redisTemplate.opsForZSet().add(femaleKey, userIdStr, weightedScore);
            }

            // 복구하면서 성별 캐시도 함께 갱신 (선택 사항)
            redisTemplate.opsForHash().put(USER_GENDER_CACHE_KEY, userIdStr, gender.name());
        }

        // 요청된 필터에 맞는 리스트만 필터링하여 반환
        List<Ranking> filteredList = rankings.stream()
                .filter(r -> {
                    if (filter == RankingFilter.ALL) {
                        return true;
                    }
                    return convertGender(r.getUser().getGender()) == filter;
                })
                .sorted(Comparator.comparingDouble(Ranking::getScore).reversed())
                .limit(TOP_RANK_LIMIT)
                .toList();

        List<Long> userIds = filteredList.stream().map(r -> r.getUser().getId()).toList();
        Map<Long, String> nicknameMap = getNicknameMap(userIds);

        List<RankingDto> responses = new ArrayList<>();
        int rank = 1;
        for (Ranking r : filteredList) {
            responses.add(RankingDto.of(
                    r.getUser().getId(),
                    nicknameMap.get(r.getUser().getId()),
                    rank++,
                    (long) Math.floor(r.getScore())));
        }

        return responses;
    }

    private List<RankingDto> convertToResponseList(Set<ZSetOperations.TypedTuple<String>> tuples,
                                                   Map<Long, String> nicknameMap) {
        List<RankingDto> result = new ArrayList<>();
        int rank = 1;
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            Long userId = Long.parseLong(Objects.requireNonNull(tuple.getValue()));
            result.add(RankingDto.of(userId, nicknameMap.get(userId), rank++,
                    (long) Math.floor(Objects.requireNonNull(tuple.getScore()))));
        }
        return result;
    }

    // 필터에 따라 Redis Key 분기 처리
    private String getRankingKey(LocalDate date, RankingFilter filter) {
        String baseKey = "ranking:daily:" + date.toString();
        if (filter == RankingFilter.MALE) {
            return baseKey + ":MALE";
        }
        if (filter == RankingFilter.FEMALE) {
            return baseKey + ":FEMALE";
        }
        return baseKey;
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

    // User 엔티티의 Gender 타입을 RankingFilter로 변환
    private RankingFilter convertGender(Object genderObj) {
        if (genderObj == null) {
            return RankingFilter.ALL;
        }

        String genderStr = String.valueOf(genderObj).toUpperCase();
        if ("MALE".equals(genderStr) || "MAN".equals(genderStr)) {
            return RankingFilter.MALE;
        }
        if ("FEMALE".equals(genderStr) || "WOMAN".equals(genderStr)) {
            return RankingFilter.FEMALE;
        }

        return RankingFilter.ALL;
    }
}