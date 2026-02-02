package com.fitpet.server.ranking.application.service;

import com.fitpet.server.ranking.application.dto.RankingSyncContext;
import com.fitpet.server.ranking.domain.entity.Ranking;
import com.fitpet.server.ranking.domain.repository.RankingRepository;
import com.fitpet.server.user.domain.repository.UserRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingSyncServiceImpl implements RankingSyncService {

    private final RankingRepository rankingRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;

    @Autowired
    @Lazy
    private RankingSyncService self;

    @Override
    public void syncRedisToDatabase() {
        RankingSyncContext context = createSyncContext(LocalDate.now());
        String dirtyKey = context.getDirtyKey();

        ScanOptions options = ScanOptions.scanOptions().count(100).build();

        try (Cursor<String> cursor = redisTemplate.opsForSet().scan(dirtyKey, options)) {
            List<String> chunk = new ArrayList<>();

            while (cursor.hasNext()) {
                chunk.add(cursor.next());

                if (chunk.size() >= 100) {
                    self.flushChunkToDatabase(chunk, context);
                    chunk.clear();
                }
            }

            if (!chunk.isEmpty()) {
                self.flushChunkToDatabase(chunk, context);
            }

        } catch (Exception e) {
            log.error("[RankingSync] 동기화 중 치명적 오류 발생", e);
        }
    }

    @Override
    @Transactional
    public void flushChunkToDatabase(List<String> userIds, RankingSyncContext context) {
        if (userIds.isEmpty()) {
            return;
        }

        Map<String, Double> scoreMap = fetchScoresInBatch(userIds, context.getRedisKey());

        List<Long> longUserIds = convertToLongIds(userIds);

        Map<Long, Ranking> existingRankings = loadExistingRankings(longUserIds, context.getDateKey());

        List<Ranking> rankingsToSave = userIds.stream()
                .map(id -> mapToRankingEntity(id, context, existingRankings, scoreMap))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        if (!rankingsToSave.isEmpty()) {
            rankingRepository.saveAll(rankingsToSave);

            redisTemplate.opsForSet().remove(context.getDirtyKey(), userIds.toArray());
        }
    }


    private Map<String, Double> fetchScoresInBatch(List<String> userIds, String redisKey) {
        List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            StringRedisConnection stringConn = (StringRedisConnection) connection;
            for (String userId : userIds) {
                stringConn.zScore(redisKey, userId);
            }
            return null;
        });

        Map<String, Double> scoreMap = new HashMap<>();
        for (int i = 0; i < userIds.size(); i++) {
            Object result = results.get(i);
            if (result instanceof Double) {
                scoreMap.put(userIds.get(i), (Double) result);
            }
        }
        return scoreMap;
    }

    private RankingSyncContext createSyncContext(LocalDate date) {
        String dateStr = date.toString();
        return RankingSyncContext.of(
                dateStr,
                "ranking:daily:" + dateStr,
                "ranking:dirty:" + dateStr
        );
    }

    private List<Long> convertToLongIds(List<String> ids) {
        return ids.stream().map(Long::parseLong).toList();
    }

    private Map<Long, Ranking> loadExistingRankings(List<Long> ids, String key) {
        return rankingRepository.findAllByUserIdInAndDateKey(ids, key).stream()
                .collect(Collectors.toMap(r -> r.getUser().getId(), r -> r));
    }

    private Optional<Ranking> mapToRankingEntity(String userIdStr, RankingSyncContext context,
                                                 Map<Long, Ranking> existingMap,
                                                 Map<String, Double> scoreMap) {

        Double redisScore = scoreMap.get(userIdStr);

        if (redisScore == null) {
            return Optional.empty();
        }

        Long userId = Long.parseLong(userIdStr);

        Ranking ranking = existingMap.getOrDefault(userId, Ranking.builder()
                .user(userRepository.getReferenceById(userId))
                .score(0)
                .dateKey(context.getDateKey())
                .build());

        ranking.updateScore(Math.floor(redisScore));
        return Optional.of(ranking);
    }
}