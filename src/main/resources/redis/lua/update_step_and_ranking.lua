-- 걸음수 Hash + 랭킹 ZSet 원자적 업데이트
-- KEYS[1]: dailywalk:steps:{date}
-- KEYS[2]: dailywalk:distance:{date}
-- KEYS[3]: dailywalk:calories:{date}
-- KEYS[4]: dailywalk:dirty:{date}
-- KEYS[5]: ranking:daily:{date} (ALL ZSet)
-- KEYS[6]: ranking:daily:{date}:GENDER (성별 ZSet, 없으면 미전달)

-- ARGV[1]: userId
-- ARGV[2]: stepDelta
-- ARGV[3]: distanceDelta (소수 문자열)
-- ARGV[4]: caloriesDelta
-- ARGV[5]: weightedScore (랭킹용 가중치 점수)
-- ARGV[6]: TTL (초)

local newSteps = redis.call('HINCRBY', KEYS[1], ARGV[1], ARGV[2])
redis.call('HINCRBYFLOAT', KEYS[2], ARGV[1], ARGV[3])
redis.call('HINCRBY', KEYS[3], ARGV[1], ARGV[4])
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

return newSteps
