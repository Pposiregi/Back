-- 랭킹 ZSet 업데이트
-- KEYS[1]: Ranking ZSet Key (예: ranking:daily:2026-01-25)
-- ARGV[1]: User ID
-- ARGV[2]: Redis Score (걸음수 + 시간가중치)
-- ARGV[3]: TTL (초 단위, 예: 259200)
redis.call('ZADD', KEYS[1], ARGV[2], ARGV[1])
redis.call('EXPIRE', KEYS[1], ARGV[3])

return 1
