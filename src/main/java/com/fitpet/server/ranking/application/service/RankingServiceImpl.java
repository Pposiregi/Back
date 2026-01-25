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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private static final long MAX_TIMESTAMP = 9_999_999_999L;
    private static final double TIME_WEIGHT_DIVIDER = 100_000_000_000.0;
    private static final int TOP_RANK_LIMIT = 10;

    @Override
    @Transactional
    public void updateScore(Long userId, int steps) {
        log.debug("[RankingService] 점수 업데이트 요청: userId={}, steps={}", userId, steps);

        LocalDate now = LocalDate.now();
        User user = getUser(userId);

        String dirtyKey = "ranking:dirty:" + now.toString();
        String redisKey = getRankingKey(now);

        long currentTimestamp = System.currentTimeMillis() / 1000;
        double redisScore = calculateTimeWeightedScore((double) steps, currentTimestamp);

        redisTemplate.execute(updateRankingScript,
                List.of(redisKey, dirtyKey),
                String.valueOf(userId), String.valueOf(redisScore)
        );

        log.info("[RankingService] Redis 업데이트 및 Dirty 플래그 완료: userId={}, finalScore={}", userId, steps);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RankingResponse> getTop10() {
        log.debug("[RankingService] 상위 10명 조회 요청");

        List<RankingResponse> result = getTop10Internal(LocalDate.now());

        log.info("[RankingService] 상위 10명 조회 완료: count={}명", result.size());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public RankingResponse getMyRank(Long userId) {
        log.debug("[RankingService] 내 랭킹 조회 요청: userId={}", userId);

        LocalDate now = LocalDate.now();
        String redisKey = getRankingKey(now);
        String userIdStr = String.valueOf(userId);

        Long rankIndex = redisTemplate.opsForZSet().reverseRank(redisKey, userIdStr);
        Double redisScore = redisTemplate.opsForZSet().score(redisKey, userIdStr);

        int finalRank;
        long finalScore;

        if (rankIndex == null || redisScore == null) {
            finalRank = calculateDefaultRank(redisKey);
            finalScore = 0L;
        } else {
            finalRank = rankIndex.intValue() + 1;
            finalScore = (long) Math.floor(redisScore);
        }

        log.info("[RankingService] 내 랭킹 조회 완료: userId={}, rank={}, score={}",
                userId, finalRank, finalScore);

        return buildRankingResponse(userId, finalRank, finalScore);
    }

    // 청크 성공을 위한 @Transactional 제거
    @Override
    public void syncAllDirtyRanksToDb() {
        LocalDate now = LocalDate.now();
        String dateKey = now.toString();
        String dirtyKey = "ranking:dirty:" + dateKey;
        String redisKey = getRankingKey(now);

        Set<String> dirtyUserIds = redisTemplate.opsForSet().members(dirtyKey);
        if (dirtyUserIds == null || dirtyUserIds.isEmpty()) {
            return;
        }

        List<String> userIdList = new ArrayList<>(dirtyUserIds);
        int batchSize = 100;

        log.info("[RankingService] DB 영속화 시작: 대상 유저={}명", userIdList.size());

        for (int i = 0; i < userIdList.size(); i += batchSize) {
            int end = Math.min(i + batchSize, userIdList.size());
            List<String> chunkIds = userIdList.subList(i, end);

            try {
                processBatchSync(chunkIds, redisKey, dirtyKey, dateKey);
            } catch (Exception e) {
                log.error("[RankingService] 배치 처리 중 오류 (Index: {}): {}", i, e.getMessage());
            }
        }
    }

    @Transactional
    public void processBatchSync(List<String> userIds, String redisKey, String dirtyKey, String dateKey) {
        List<Long> longUserIds = userIds.stream().map(Long::parseLong).toList();

        Map<Long, Ranking> existingRankings = rankingRepository.findAllByUserIdInAndDateKey(longUserIds, dateKey)
                .stream()
                .collect(Collectors.toMap(r -> r.getUser().getId(), r -> r));

        List<Ranking> toSave = new ArrayList<>();

        for (Long userId : longUserIds) {
            Double redisScore = redisTemplate.opsForZSet().score(redisKey, String.valueOf(userId));

            if (redisScore != null) {
                int totalSteps = (int) Math.floor(redisScore);
                Ranking ranking = existingRankings.get(userId);

                if (ranking == null) {
                    User userProxy = userRepository.getReferenceById(userId);
                    ranking = Ranking.builder()
                            .user(userProxy)
                            .score(0)
                            .dateKey(dateKey)
                            .build();
                }

                ranking.updateScore((double) totalSteps);
                toSave.add(ranking);
            }
        }

        if (!toSave.isEmpty()) {
            rankingRepository.saveAll(toSave);
        }

        redisTemplate.opsForSet().remove(dirtyKey, userIds.toArray());

        log.debug("[RankingService] 배치 동기화 완료: {}명", toSave.size());
    }

    private Set<String> getDirtyUserIds(String dirtyKey) {
        return redisTemplate.opsForSet().members(dirtyKey);
    }


    private List<RankingResponse> getTop10Internal(LocalDate now) {
        String redisKey = getRankingKey(now);

        Set<ZSetOperations.TypedTuple<String>> tuples =
                redisTemplate.opsForZSet().reverseRangeWithScores(redisKey, 0, TOP_RANK_LIMIT - 1);

        if (tuples == null || tuples.isEmpty()) {
            return refreshRankingFromDb(now);
        }

        return convertToResponseList(tuples);
    }

    private List<RankingResponse> refreshRankingFromDb(LocalDate now) {
        String dateKey = getDateKey(now);
        String redisKey = getRankingKey(now);

        log.warn("[RankingService] Redis 캐시 미스 - DB 복구 시작: dateKey={}", dateKey);

        List<Ranking> rankings = rankingRepository.findAllByDateKey(dateKey);

        for (Ranking r : rankings) {
            long originalTimestamp = (r.getUpdatedAt() != null)
                    ? r.getUpdatedAt().atZone(ZoneId.systemDefault()).toEpochSecond()
                    : System.currentTimeMillis() / 1000;

            double redisScore = calculateTimeWeightedScore(r.getScore(), originalTimestamp);
            redisTemplate.opsForZSet().add(redisKey, String.valueOf(r.getUser().getId()), redisScore);
        }

        log.info("[RankingService] Redis 데이터 복구 완료: 복구된 인원={}명", rankings.size());

        return getTop10Internal(now);
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
        double newTotalScore = (double) steps;
        ranking.updateScore(newTotalScore);
        rankingRepository.save(ranking);
        return newTotalScore;
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("[RankingService] 사용자 조회 실패: userId={}", userId);
                    return new IllegalArgumentException("User not found");
                });
    }

    private String getRankingKey(LocalDate date) {
        return "ranking:daily:" + date.toString();
    }

    private String getDateKey(LocalDate date) {
        return date.toString();
    }

    private List<RankingResponse> convertToResponseList(Set<ZSetOperations.TypedTuple<String>> tuples) {
        List<RankingResponse> result = new ArrayList<>();
        int rank = 1;

        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            String userIdStr = tuple.getValue();
            Double redisScore = tuple.getScore();

            if (userIdStr != null && redisScore != null) {
                long realScore = (long) Math.floor(redisScore);

                result.add(buildRankingResponse(Long.parseLong(userIdStr), rank++, realScore));
            }
        }

        return result;
    }

    private int calculateDefaultRank(String redisKey) {
        Long totalParticipants = redisTemplate.opsForZSet().size(redisKey);
        return (totalParticipants != null ? totalParticipants.intValue() : 0) + 1;
    }

    private RankingResponse buildRankingResponse(Long userId, int rank, long score) {
        return RankingResponse.builder()
                .rank(rank)
                .userId(userId)
                .score(score)
                .build();
    }
}