-- 걸음수 Hash + 랭킹 ZSet 원자적 업데이트 (총합 SET 방식)
-- 프론트가 항상 최신 총합을 전송하므로 HINCRBY 대신 HSET 사용
-- KEYS[1]: dailywalk:steps:{date}
-- KEYS[2]: dailywalk:distance:{date}
-- KEYS[3]: dailywalk:calories:{date}
-- KEYS[4]: dailywalk:dirty:{date}
-- KEYS[5]: ranking:daily:{date} (ALL ZSet)
-- KEYS[6]: ranking:daily:{date}:GENDER (성별 ZSet, 없으면 미전달)

-- ARGV[1]: userId
-- ARGV[2]: totalSteps (총 걸음수)
-- ARGV[3]: totalDistance (총 거리, 소수 문자열)
-- ARGV[4]: totalCalories (총 칼로리)
-- ARGV[5]: weightedScore (랭킹용 가중치 점수)
-- ARGV[6]: TTL (초)

-- 추월 감지를 위해 ZADD 이전 순위를 원자적으로 스냅샷
-- ZREVRANK: 0-indexed (0 = 1위). 처음 진입이면 nil → -1 로 치환
local prev_rank = redis.call('ZREVRANK', KEYS[5], ARGV[1])

redis.call('HSET', KEYS[1], ARGV[1], ARGV[2])
redis.call('HSET', KEYS[2], ARGV[1], ARGV[3])
redis.call('HSET', KEYS[3], ARGV[1], ARGV[4])
redis.call('SADD', KEYS[4], ARGV[1])
redis.call('ZADD', KEYS[5], ARGV[5], ARGV[1])

redis.call('EXPIRE', KEYS[1], ARGV[6])
redis.call('EXPIRE', KEYS[2], ARGV[6])
redis.call('EXPIRE', KEYS[3], ARGV[6])
redis.call('EXPIRE', KEYS[4], ARGV[6])
redis.call('EXPIRE', KEYS[5], ARGV[6])

if KEYS[6] then
    redis.call('ZADD', KEYS[6], ARGV[5], ARGV[1])
    redis.call('EXPIRE', KEYS[6], ARGV[6])
end

-- {이전 순위(없으면 -1), totalSteps} 반환
return {prev_rank or -1, tonumber(ARGV[2])}
