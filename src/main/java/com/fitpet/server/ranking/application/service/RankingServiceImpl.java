package com.fitpet.server.ranking.application.service;

import com.fitpet.server.ranking.application.dto.RankingDto;
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
        String dirtyKey = getModifiedUsersKey(now);

        double weightedScore = calculateTimeWeightedScore((double) steps, System.currentTimeMillis() / 1000);
        String userIdStr = String.valueOf(userId);

        RankingFilter userGender = getUserGender(userId);

        String allRankingKey = getRankingKey(now, RankingFilter.ALL);
        String genderRankingKey = getRankingKey(now, userGender);

        redisTemplate.execute(updateRankingScript,
                List.of(allRankingKey, dirtyKey),
                userIdStr,
                String.valueOf(weightedScore),
                TTL_SECONDS
        );

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
            String imageUrl = (String) cachedImages.get(i);

            if (nickname == null) {
                missIds.add(userId);
            } else {
                profileMap.put(userId, new UserProfileInfo(nickname, imageUrl));
            }
        }

        if (!missIds.isEmpty()) {
            List<User> missingUsers = userRepository.findAllById(missIds);
            Map<String, String> nameUpdates = new HashMap<>();
            Map<String, String> imageUpdates = new HashMap<>();

            for (User u : missingUsers) {
                String img = u.getProfileImageUrl() != null ? u.getProfileImageUrl() : "";
                profileMap.put(u.getId(), new UserProfileInfo(u.getNickname(), img));

                nameUpdates.put(String.valueOf(u.getId()), u.getNickname());
                imageUpdates.put(String.valueOf(u.getId()), img);
            }

            if (!nameUpdates.isEmpty()) {
                redisTemplate.opsForHash().putAll(USER_PROFILE_KEY, nameUpdates);
                redisTemplate.opsForHash().putAll(USER_IMAGE_KEY, imageUpdates);
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
        String dateKey = now.toString();
        log.warn("[RankingService] 캐시 미스 - DB 데이터 복구 시도: {}", dateKey);

        List<Ranking> rankings = rankingRepository.findTopRankings(dateKey, PageRequest.of(0, 100));

        if (rankings.isEmpty()) {
            return Collections.emptyList();
        }

        String allKey = getRankingKey(now, RankingFilter.ALL);
        String maleKey = getRankingKey(now, RankingFilter.MALE);
        String femaleKey = getRankingKey(now, RankingFilter.FEMALE);

        Map<String, String> nameUpdates = new HashMap<>();
        Map<String, String> imageUpdates = new HashMap<>();
        Map<String, String> genderUpdates = new HashMap<>();

        for (Ranking r : rankings) {
            long ts = (r.getUpdatedAt() != null)
                    ? r.getUpdatedAt().atZone(ZoneId.systemDefault()).toEpochSecond()
                    : System.currentTimeMillis() / 1000;

            double weightedScore = calculateTimeWeightedScore(r.getScore(), ts);
            String userIdStr = String.valueOf(r.getUser().getId());
            RankingFilter gender = convertGender(r.getUser().getGender());

            redisTemplate.opsForZSet().add(allKey, userIdStr, weightedScore);
            if (gender == RankingFilter.MALE) {
                redisTemplate.opsForZSet().add(maleKey, userIdStr, weightedScore);
            } else if (gender == RankingFilter.FEMALE) {
                redisTemplate.opsForZSet().add(femaleKey, userIdStr, weightedScore);
            }

            genderUpdates.put(userIdStr, gender.name());
            nameUpdates.put(userIdStr, r.getUser().getNickname());

            String img = r.getUser().getProfileImageUrl() != null ? r.getUser().getProfileImageUrl() : "";
            imageUpdates.put(userIdStr, img);
        }

        if (!nameUpdates.isEmpty()) {
            redisTemplate.opsForHash().putAll(USER_PROFILE_KEY, nameUpdates);
            redisTemplate.opsForHash().putAll(USER_IMAGE_KEY, imageUpdates);
            redisTemplate.opsForHash().putAll(USER_GENDER_CACHE_KEY, genderUpdates);
        }

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

        List<RankingDto> responses = new ArrayList<>();
        int rank = 1;
        for (Ranking r : filteredList) {
            responses.add(RankingDto.of(
                    r.getUser().getId(),
                    r.getUser().getNickname(),
                    r.getUser().getProfileImageUrl(),
                    rank++,
                    (long) Math.floor(r.getScore())));
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