package com.fitpet.server.ranking.application.service;

import com.fitpet.server.dailywalk.domain.entity.DailyWalk;
import com.fitpet.server.dailywalk.domain.repository.DailyWalkRepository;
import com.fitpet.server.ranking.application.dto.RankingDto;
import com.fitpet.server.ranking.application.event.RankingScoreUpdatedEvent;
import com.fitpet.server.ranking.domain.type.RankingFilter;
import com.fitpet.server.shared.s3.S3Service;
import com.fitpet.server.user.domain.entity.User;
import com.fitpet.server.user.domain.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    private final DailyWalkRepository dailyWalkRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final StringRedisTemplate redisTemplate;
    private final RedisScript<Long> updateRankingScript;
    @SuppressWarnings("rawtypes")
    private final RedisScript<List> updateStepAndRankingScript;
    private final ApplicationEventPublisher eventPublisher;

    private static final String DAILYWALK_STEPS_KEY = "dailywalk:steps:";
    private static final String DAILYWALK_DISTANCE_KEY = "dailywalk:distance:";
    private static final String DAILYWALK_CALORIES_KEY = "dailywalk:calories:";
    private static final String DAILYWALK_DIRTY_KEY = "dailywalk:dirty:";

    private static final String USER_PROFILE_KEY = "user:profiles";
    private static final String USER_IMAGE_KEY = "user:images";
    private static final String USER_GENDER_CACHE_KEY = "user:genders";

    private static final long MAX_TIMESTAMP = 9_999_999_999L;
    private static final double TIME_WEIGHT_DIVIDER = 100_000_000_000.0;
    private static final int TOP_RANK_LIMIT = 10;
    private static final String TTL_SECONDS = "259200";

    private record UserProfileInfo(String nickname, String imageUrl) {
    }

    @Override
    @Transactional
    public void updateScore(Long userId, int steps) {
        LocalDate now = LocalDate.now();
        double weightedScore = calculateTimeWeightedScore((double) steps, System.currentTimeMillis() / 1000);
        String userIdStr = String.valueOf(userId);

        RankingFilter userGender = getUserGender(userId);

        String allRankingKey = getRankingKey(now, RankingFilter.ALL);
        String genderRankingKey = getRankingKey(now, userGender);

        redisTemplate.execute(updateRankingScript,
                List.of(allRankingKey),
                userIdStr,
                String.valueOf(weightedScore),
                TTL_SECONDS
        );

        if (userGender != RankingFilter.ALL) {
            redisTemplate.execute(updateRankingScript,
                    List.of(genderRankingKey),
                    userIdStr,
                    String.valueOf(weightedScore),
                    TTL_SECONDS
            );
        }

        log.info("[RankingService] 랭킹 업데이트 완료 (Lua): userId={}, score={}, gender={}", userId, weightedScore,
                userGender);
    }

    @Override
    @Transactional
    public long updateStepHashAndRankingScore(Long userId, int totalSteps,
                                              BigDecimal totalDistance, int totalCalories,
                                              LocalDate date) {
        String dateStr = date.toString();
        String userIdStr = String.valueOf(userId);
        double weightedScore = calculateTimeWeightedScore(totalSteps, System.currentTimeMillis() / 1000);
        RankingFilter gender = getUserGender(userId);

        String allRankingKey = getRankingKey(date, RankingFilter.ALL);

        List<String> keys = (gender != RankingFilter.ALL)
                ? List.of(DAILYWALK_STEPS_KEY + dateStr, DAILYWALK_DISTANCE_KEY + dateStr,
                          DAILYWALK_CALORIES_KEY + dateStr, DAILYWALK_DIRTY_KEY + dateStr,
                          allRankingKey, getRankingKey(date, gender))
                : List.of(DAILYWALK_STEPS_KEY + dateStr, DAILYWALK_DISTANCE_KEY + dateStr,
                          DAILYWALK_CALORIES_KEY + dateStr, DAILYWALK_DIRTY_KEY + dateStr,
                          allRankingKey);

        @SuppressWarnings({"rawtypes", "unchecked"})
        List<Long> result = (List<Long>) redisTemplate.execute(updateStepAndRankingScript,
                keys,
                userIdStr,
                String.valueOf(totalSteps),
                totalDistance.toPlainString(),
                String.valueOf(totalCalories),
                String.valueOf(weightedScore),
                TTL_SECONDS
        );

        Long rawPrevRank = (result != null && !result.isEmpty()) ? result.get(0) : null;
        Long previousRank = (rawPrevRank == null || rawPrevRank == -1L) ? null : rawPrevRank;
        long resultSteps = (result != null && result.size() > 1 && result.get(1) != null)
                ? result.get(1) : totalSteps;

        log.info("[RankingService] 걸음수 Hash + 랭킹 ZSet SET 업데이트: userId={}, totalSteps={}", userId, totalSteps);

        eventPublisher.publishEvent(new RankingScoreUpdatedEvent(userId, previousRank, date));

        return resultSteps;
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
        String redisKey = getRankingKey(now, filter);
        String userIdStr = String.valueOf(userId);

        Long rankIndex = redisTemplate.opsForZSet().reverseRank(redisKey, userIdStr);
        Double redisScore = redisTemplate.opsForZSet().score(redisKey, userIdStr);

        int finalRank = (rankIndex == null) ? calculateDefaultRank(redisKey) : rankIndex.intValue() + 1;
        long finalScore = (redisScore == null) ? 0L : (long) Math.floor(redisScore);

        Map<Long, UserProfileInfo> profileMap = getUserProfileMap(Collections.singletonList(userId));
        UserProfileInfo info = profileMap.getOrDefault(userId, new UserProfileInfo(null, null));

        return RankingDto.of(userId, info.nickname(), info.imageUrl(), finalRank, finalScore);
    }

    private RankingFilter getUserGender(Long userId) {
        String userIdStr = String.valueOf(userId);

        Object cachedGender = redisTemplate.opsForHash().get(USER_GENDER_CACHE_KEY, userIdStr);
        if (cachedGender != null) {
            return RankingFilter.valueOf((String) cachedGender);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        RankingFilter filter = convertGender(user.getGender());

        redisTemplate.opsForHash().put(USER_GENDER_CACHE_KEY, userIdStr, filter.name());

        return filter;
    }

    private Map<Long, UserProfileInfo> getUserProfileMap(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<String> userIdsStr = userIds.stream().map(String::valueOf).toList();
        List<Object> cachedNames = redisTemplate.opsForHash().multiGet(USER_PROFILE_KEY, new ArrayList<>(userIdsStr));
        List<Object> cachedImages = redisTemplate.opsForHash().multiGet(USER_IMAGE_KEY, new ArrayList<>(userIdsStr));

        Map<Long, UserProfileInfo> profileMap = new HashMap<>();
        List<Long> missIds = new ArrayList<>();

        for (int i = 0; i < userIds.size(); i++) {
            Long userId = userIds.get(i);
            String nickname = (String) cachedNames.get(i);
            String imageKey = (String) cachedImages.get(i);

            if (nickname == null || imageKey == null || imageKey.isBlank()) {
                missIds.add(userId);
            } else {
                String viewableUrl = s3Service.generatePresignedGetUrl(imageKey);
                profileMap.put(userId, new UserProfileInfo(nickname, viewableUrl));
            }
        }

        if (!missIds.isEmpty()) {
            log.warn("[RankingService] user:profiles 캐시 미스 — DB 동기 조회 발생: userIds={}", missIds);
            List<User> missingUsers = userRepository.findAllById(missIds);
            for (User u : missingUsers) {
                String dbImgKey = u.getProfileImageUrl();
                String viewableUrl = (dbImgKey != null && !dbImgKey.isBlank())
                        ? s3Service.generatePresignedGetUrl(dbImgKey) : "";

                profileMap.put(u.getId(), new UserProfileInfo(u.getNickname(), viewableUrl));

                redisTemplate.opsForHash()
                        .put(USER_IMAGE_KEY, String.valueOf(u.getId()), dbImgKey != null ? dbImgKey : "");
                redisTemplate.opsForHash().put(USER_PROFILE_KEY, String.valueOf(u.getId()), u.getNickname());
            }
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
        Map<Long, UserProfileInfo> profileMap = getUserProfileMap(userIds);

        return convertToResponseList(tuples, profileMap);
    }

    private List<RankingDto> recoverRedisFromDatabase(LocalDate now, RankingFilter filter) {
        log.warn("[RankingService] 캐시 미스 - daily_walk DB 데이터 복구 시도: {}", now);

        LocalDateTime startOfDay = now.atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        List<DailyWalk> walks = dailyWalkRepository.findAllByCreatedAtBetween(startOfDay, endOfDay);

        if (walks.isEmpty()) {
            return Collections.emptyList();
        }

        String allKey = getRankingKey(now, RankingFilter.ALL);
        String maleKey = getRankingKey(now, RankingFilter.MALE);
        String femaleKey = getRankingKey(now, RankingFilter.FEMALE);

        Map<String, String> nameUpdates = new HashMap<>();
        Map<String, String> imageUpdates = new HashMap<>();
        Map<String, String> genderUpdates = new HashMap<>();

        long ts = System.currentTimeMillis() / 1000;

        for (DailyWalk walk : walks) {
            User u = walk.getUser();
            double weightedScore = calculateTimeWeightedScore(walk.getStep(), ts);
            String userIdStr = String.valueOf(u.getId());
            RankingFilter gender = convertGender(u.getGender());

            redisTemplate.opsForZSet().add(allKey, userIdStr, weightedScore);
            if (gender == RankingFilter.MALE) {
                redisTemplate.opsForZSet().add(maleKey, userIdStr, weightedScore);
            } else if (gender == RankingFilter.FEMALE) {
                redisTemplate.opsForZSet().add(femaleKey, userIdStr, weightedScore);
            }

            genderUpdates.put(userIdStr, gender.name());
            nameUpdates.put(userIdStr, u.getNickname());
            imageUpdates.put(userIdStr, u.getProfileImageUrl() != null ? u.getProfileImageUrl() : "");
        }

        if (!nameUpdates.isEmpty()) {
            redisTemplate.opsForHash().putAll(USER_PROFILE_KEY, nameUpdates);
            redisTemplate.opsForHash().putAll(USER_IMAGE_KEY, imageUpdates);
            redisTemplate.opsForHash().putAll(USER_GENDER_CACHE_KEY, genderUpdates);
        }

        List<DailyWalk> filteredList = walks.stream()
                .filter(walk -> {
                    if (filter == RankingFilter.ALL) return true;
                    return convertGender(walk.getUser().getGender()) == filter;
                })
                .sorted(Comparator.comparingInt(DailyWalk::getStep).reversed())
                .limit(TOP_RANK_LIMIT)
                .toList();

        List<RankingDto> responses = new ArrayList<>();
        int rank = 1;
        for (DailyWalk walk : filteredList) {
            User u = walk.getUser();
            String dbImgKey = u.getProfileImageUrl();
            String viewableUrl = (dbImgKey != null && !dbImgKey.isBlank())
                    ? s3Service.generatePresignedGetUrl(dbImgKey) : "";

            responses.add(RankingDto.of(u.getId(), u.getNickname(), viewableUrl, rank++, (long) walk.getStep()));
        }

        return responses;
    }

    private List<RankingDto> convertToResponseList(Set<ZSetOperations.TypedTuple<String>> tuples,
                                                   Map<Long, UserProfileInfo> profileMap) {
        List<RankingDto> result = new ArrayList<>();
        int rank = 1;
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            Long userId = Long.parseLong(Objects.requireNonNull(tuple.getValue()));
            UserProfileInfo info = profileMap.getOrDefault(userId, new UserProfileInfo(null, null));

            result.add(RankingDto.of(
                    userId,
                    info.nickname(),
                    info.imageUrl(),
                    rank++,
                    (long) Math.floor(Objects.requireNonNull(tuple.getScore()))
            ));
        }
        return result;
    }

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

    private double calculateTimeWeightedScore(double s, long ts) {
        return s + (double) (MAX_TIMESTAMP - ts) / TIME_WEIGHT_DIVIDER;
    }

    private int calculateDefaultRank(String k) {
        Long s = redisTemplate.opsForZSet().size(k);
        return (s != null ? s.intValue() : 0) + 1;
    }

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
