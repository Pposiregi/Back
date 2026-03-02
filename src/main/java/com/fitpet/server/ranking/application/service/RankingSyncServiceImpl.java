package com.fitpet.server.ranking.application.service;

import com.fitpet.server.dailywalk.domain.repository.DailyWalkRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingSyncServiceImpl implements RankingSyncService {

    private final DailyWalkRepository dailyWalkRepository;
    private final StringRedisTemplate redisTemplate;

    @Autowired
    @Lazy
    private RankingSyncService self;

    private static final String DAILYWALK_STEPS_KEY = "dailywalk:steps:";
    private static final String DAILYWALK_DISTANCE_KEY = "dailywalk:distance:";
    private static final String DAILYWALK_CALORIES_KEY = "dailywalk:calories:";
    private static final String DAILYWALK_DIRTY_KEY = "dailywalk:dirty:";

    @Override
    public void syncRedisToDatabase() {
        String dateStr = LocalDate.now().toString();
        String dirtyKey = DAILYWALK_DIRTY_KEY + dateStr;

        ScanOptions options = ScanOptions.scanOptions().count(100).build();

        try (Cursor<String> cursor = redisTemplate.opsForSet().scan(dirtyKey, options)) {
            List<String> chunk = new ArrayList<>();

            while (cursor.hasNext()) {
                chunk.add(cursor.next());

                if (chunk.size() >= 100) {
                    self.flushChunkToDatabase(chunk, dateStr);
                    chunk.clear();
                }
            }

            if (!chunk.isEmpty()) {
                self.flushChunkToDatabase(chunk, dateStr);
            }

        } catch (Exception e) {
            log.error("[DailyWalkSync] 동기화 중 치명적 오류 발생", e);
        }
    }

    @Override
    @Transactional
    public void flushChunkToDatabase(List<String> userIdStrs, String dateStr) {
        if (userIdStrs.isEmpty()) {
            return;
        }

        String stepsKey = DAILYWALK_STEPS_KEY + dateStr;
        String distanceKey = DAILYWALK_DISTANCE_KEY + dateStr;
        String caloriesKey = DAILYWALK_CALORIES_KEY + dateStr;
        String dirtyKey = DAILYWALK_DIRTY_KEY + dateStr;

        LocalDateTime startOfDay = LocalDate.parse(dateStr).atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        for (String userIdStr : userIdStrs) {
            try {
                Long userId = Long.parseLong(userIdStr);

                Object steps = redisTemplate.opsForHash().get(stepsKey, userIdStr);
                Object distance = redisTemplate.opsForHash().get(distanceKey, userIdStr);
                Object calories = redisTemplate.opsForHash().get(caloriesKey, userIdStr);

                if (steps == null) {
                    log.warn("[DailyWalkSync] Redis 데이터 없음 - 건너뜀: userId={}, date={}", userId, dateStr);
                    redisTemplate.opsForSet().remove(dirtyKey, userIdStr);
                    continue;
                }

                int totalSteps = Integer.parseInt((String) steps);
                BigDecimal totalDistance = new BigDecimal(distance != null ? (String) distance : "0");
                int totalCalories = calories != null ? Integer.parseInt((String) calories) : 0;

                int updated = dailyWalkRepository.updateStepByUserIdAndDate(
                        userId, startOfDay, endOfDay, totalSteps, totalDistance, totalCalories);

                if (updated == 0) {
                    log.warn("[DailyWalkSync] DB 업데이트 실패(레코드 없음): userId={}, date={}", userId, dateStr);
                } else {
                    redisTemplate.opsForSet().remove(dirtyKey, userIdStr);
                    log.debug("[DailyWalkSync] 동기화 완료: userId={}, steps={}", userId, totalSteps);
                }

            } catch (Exception e) {
                log.error("[DailyWalkSync] userId={} 동기화 실패", userIdStr, e);
            }
        }
    }
}
