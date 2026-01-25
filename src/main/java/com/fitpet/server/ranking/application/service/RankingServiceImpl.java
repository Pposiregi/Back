package com.fitpet.server.ranking.application.service;

import com.fitpet.server.ranking.application.dto.RankingSyncContext;
import com.fitpet.server.ranking.domain.entity.Ranking;
import com.fitpet.server.ranking.domain.repository.RankingRepository;
import com.fitpet.server.ranking.presentation.dto.RankingResponse;
import com.fitpet.server.user.domain.entity.User;
import com.fitpet.server.user.domain.repository.UserRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

        return buildRankingResponse(userId, finalRank, finalScore);
    }

    @Override
    public void syncRedisToDatabase() {
        RankingSyncContext context = createSyncContext(LocalDate.now());
        List<String> modifiedUserIds = getModifiedUserIds(context.getDirtyKey());

        if (modifiedUserIds.isEmpty()) {
            return;
        }

        log.info("[RankingSync] DB 동기화 시작: 대상={}명", modifiedUserIds.size());
        transferInChunks(modifiedUserIds, context);
    }

    // Chunk 단위로 나누어 동기화 작업 분배
    private void transferInChunks(List<String> userIds, RankingSyncContext context) {
        int chunkSize = 100;
        for (int i = 0; i < userIds.size(); i += chunkSize) {
            List<String> chunk = userIds.subList(i, Math.min(i + chunkSize, userIds.size()));
            try {
                flushChunkToDatabase(chunk, context);
            } catch (Exception e) {
                log.error("[RankingSync] 덩어리 처리 중 오류 (Index: {}): {}", i, e.getMessage());
            }
        }
    }

    // 실제 DB 저장 및 Redis 상태 정리 (트랜잭션 단위)
    @Transactional
    public void flushChunkToDatabase(List<String> userIds, RankingSyncContext context) {
        List<Long> longUserIds = convertToLongIds(userIds);
        Map<Long, Ranking> existingRankings = loadExistingRankings(longUserIds, context.getDateKey());

        List<Ranking> rankingsToSave = userIds.stream()
                .map(id -> mapToRankingEntity(id, context, existingRankings))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        saveAndClearModifiedFlags(rankingsToSave, userIds, context.getDirtyKey());
    }

    // Redis 정보를 바탕으로 DB 엔티티로 변환
    private Optional<Ranking> mapToRankingEntity(String userIdStr, RankingSyncContext context,
                                                 Map<Long, Ranking> existingMap) {
        Double redisScore = redisTemplate.opsForZSet().score(context.getRedisKey(), userIdStr);
        if (redisScore == null) {
            return Optional.empty();
        }

        Long userId = Long.parseLong(userIdStr);
        Ranking ranking = existingMap.getOrDefault(userId, createProxyRanking(userId, context.getDateKey()));

        ranking.updateScore(Math.floor(redisScore));
        return Optional.of(ranking);
    }

    // 유저 테이블 조회를 방지하기 위한 Proxy 생성
    private Ranking createProxyRanking(Long userId, String dateKey) {
        return Ranking.builder()
                .user(userRepository.getReferenceById(userId))
                .score(0)
                .dateKey(dateKey)
                .build();
    }

    // DB 저장 후 성공 시에만 Redis의 수정 플래그 삭제
    private void saveAndClearModifiedFlags(List<Ranking> rankings, List<String> userIds, String dirtyKey) {
        if (!rankings.isEmpty()) {
            rankingRepository.saveAll(rankings);
            redisTemplate.opsForSet().remove(dirtyKey, userIds.toArray());
            log.debug("[RankingSync] {}명 동기화 완료", rankings.size());
        }
    }

    private List<RankingResponse> fetchTopRankings(LocalDate now) {
        String redisKey = getRankingKey(now);
        Set<ZSetOperations.TypedTuple<String>> tuples =
                redisTemplate.opsForZSet().reverseRangeWithScores(redisKey, 0, TOP_RANK_LIMIT - 1);

        if (tuples == null || tuples.isEmpty()) {
            return recoverRedisFromDatabase(now);
        }
        return convertToResponseList(tuples);
    }

    private List<RankingResponse> recoverRedisFromDatabase(LocalDate now) {
        String dateKey = now.toString();
        log.warn("[RankingService] 캐시 미스 - DB 데이터 복구: {}", dateKey);

        List<Ranking> rankings = rankingRepository.findAllByDateKey(dateKey);
        for (Ranking r : rankings) {
            long timestamp = (r.getUpdatedAt() != null)
                    ? r.getUpdatedAt().atZone(ZoneId.systemDefault()).toEpochSecond()
                    : System.currentTimeMillis() / 1000;

            double score = calculateTimeWeightedScore(r.getScore(), timestamp);
            redisTemplate.opsForZSet().add(getRankingKey(now), String.valueOf(r.getUser().getId()), score);
        }
        return fetchTopRankings(now);
    }

    private List<String> getModifiedUserIds(String dirtyKey) {
        Set<String> members = redisTemplate.opsForSet().members(dirtyKey);
        return (members != null) ? new ArrayList<>(members) : Collections.emptyList();
    }

    private RankingSyncContext createSyncContext(LocalDate date) {
        return RankingSyncContext.of(date.toString(), getRankingKey(date), getModifiedUsersKey(date));
    }

    private Map<Long, Ranking> loadExistingRankings(List<Long> userIds, String dateKey) {
        return rankingRepository.findAllByUserIdInAndDateKey(userIds, dateKey)
                .stream()
                .collect(Collectors.toMap(r -> r.getUser().getId(), r -> r));
    }

    private String getRankingKey(LocalDate date) {
        return "ranking:daily:" + date.toString();
    }

    private String getModifiedUsersKey(LocalDate date) {
        return "ranking:dirty:" + date.toString();
    }

    private List<Long> convertToLongIds(List<String> userIds) {
        return userIds.stream().map(Long::parseLong).toList();
    }

    private double calculateTimeWeightedScore(double score, long ts) {
        return score + (double) (MAX_TIMESTAMP - ts) / TIME_WEIGHT_DIVIDER;
    }

    private int calculateDefaultRank(String key) {
        Long size = redisTemplate.opsForZSet().size(key);
        return (size != null ? size.intValue() : 0) + 1;
    }

    private RankingResponse buildRankingResponse(Long userId, int rank, long score) {
        return RankingResponse.builder().rank(rank).userId(userId).score(score).build();
    }

    private List<RankingResponse> convertToResponseList(Set<ZSetOperations.TypedTuple<String>> tuples) {
        List<RankingResponse> result = new ArrayList<>();
        int rank = 1;
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            if (tuple.getValue() != null && tuple.getScore() != null) {
                result.add(buildRankingResponse(Long.parseLong(tuple.getValue()), rank++,
                        (long) Math.floor(tuple.getScore())));
            }
        }
        return result;
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}