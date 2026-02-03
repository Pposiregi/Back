-- 랭킹 업데이트
-- KEYS[1]: Ranking ZSet Key (예: ranking:daily:2026-01-25)
-- KEYS[2]: Dirty Set Key (예: ranking:dirty:2026-01-25)
-- ARGV[1]: User ID
-- ARGV[2]: Redis Score (걸음수 + 시간가중치)
-- ARGV[3]: TTL (초 단위, 예: 259200)
redis.call('ZADD', KEYS[1], ARGV[2], ARGV[1])

-- DB 영속화
-- KEYS[2]: Dirty 필래그 키 (예: ranking:dirty:2026-01-24)
-- ARGV[1]: 유저 ID
redis.call('SADD', KEYS[2], ARGV[1])

-- 두 키 모두 TTL 적용 - 이미 존재하면 갱신됨
redis.call('EXPIRE', KEYS[1], ARGV[3])
redis.call('EXPIRE', KEYS[2], ARGV[3])

return 1