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

    @Override
    public void syncRedisToDatabase() {
        RankingSyncContext context = createSyncContext(LocalDate.now());
        String dirtyKey = context.getDirtyKey();

        // Non-blocking 방식을 위한 SSCAN 설정 (100개 단위)
        ScanOptions options = ScanOptions.scanOptions().count(100).build();

        try (Cursor<String> cursor = redisTemplate.opsForSet().scan(dirtyKey, options)) {
            List<String> chunk = new ArrayList<>();

            while (cursor.hasNext()) {
                chunk.add(cursor.next());

                // 100개가 모이면 DB에 저장 (Chunking)
                if (chunk.size() >= 100) {
                    flushChunkToDatabase(chunk, context);
                    chunk.clear();
                }
            }

            // 남은 자투리 데이터 처리
            if (!chunk.isEmpty()) {
                flushChunkToDatabase(chunk, context);
            }

        } catch (Exception e) {
            log.error("[RankingSync] 동기화 중 치명적 오류 발생", e);
        }
    }

    /**
     * 청크 단위 데이터를 DB에 영속화하고 Dirty Set에서 제거 - 개선점: Redis Pipelining을 사용하여 점수 조회 시 네트워크 RTT를 획기적으로 줄임
     */
    @Transactional
    public void flushChunkToDatabase(List<String> userIds, RankingSyncContext context) {
        if (userIds.isEmpty()) {
            return;
        }

        // [New] 1. Redis Pipelining: 청크 내 모든 유저의 점수를 한 번에 조회 (RTT 1회)
        Map<String, Double> scoreMap = fetchScoresInBatch(userIds, context.getRedisKey());

        List<Long> longUserIds = convertToLongIds(userIds);

        // 2. 기존 DB 데이터 조회 (Bulk Select)
        Map<Long, Ranking> existingRankings = loadExistingRankings(longUserIds, context.getDateKey());

        // 3. 엔티티 매핑 (Update or Create with Proxy)
        // mapToRankingEntity에 미리 가져온 scoreMap을 전달하여 추가적인 Redis 조회를 방지
        List<Ranking> rankingsToSave = userIds.stream()
                .map(id -> mapToRankingEntity(id, context, existingRankings, scoreMap))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        if (!rankingsToSave.isEmpty()) {
            // 4. DB 저장 (Batch Insert/Update)
            rankingRepository.saveAll(rankingsToSave);

            // 5. Redis Dirty Set에서 제거 (처리 완료)
            redisTemplate.opsForSet().remove(context.getDirtyKey(), userIds.toArray());
        }
    }

    /**
     * [Pipelining 구현부] 여러 유저의 ZScore 명령어를 파이프라인으로 묶어 한 번에 실행합니다. 100번의 네트워크 통신을 1번으로 줄여줍니다.
     */
    private Map<String, Double> fetchScoresInBatch(List<String> userIds, String redisKey) {
        List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            StringRedisConnection stringConn = (StringRedisConnection) connection;
            for (String userId : userIds) {
                stringConn.zScore(redisKey, userId);
            }
            return null;
        });

        // 결과 List를 Map<UserId, Score>로 변환 (O(1) 조회를 위함)
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

    // [Modified] Redis를 직접 호출하지 않고, 전달받은 scoreMap을 사용하도록 변경
    private Optional<Ranking> mapToRankingEntity(String userIdStr, RankingSyncContext context,
                                                 Map<Long, Ranking> existingMap,
                                                 Map<String, Double> scoreMap) {

        // 미리 조회해둔 Map에서 점수 획득 (메모리 연산)
        Double redisScore = scoreMap.get(userIdStr);

        if (redisScore == null) {
            return Optional.empty();
        }

        Long userId = Long.parseLong(userIdStr);

        // Zero-Select 전략: getReferenceById를 사용해 SELECT 없이 Proxy 객체 생성
        Ranking ranking = existingMap.getOrDefault(userId, Ranking.builder()
                .user(userRepository.getReferenceById(userId))
                .score(0)
                .dateKey(context.getDateKey())
                .build());

        ranking.updateScore(Math.floor(redisScore));
        return Optional.of(ranking);
    }
}