-- 랭킹 업데이트
-- KEYS[1]: 랭킹 키 (예: ranking:daily:2026-01-24)
-- ARGV[2]: 가중치가 적용된 점수 (Score)
-- ARGV[1]: 유저 ID
redis.call('ZADD', KEYS[1], ARGV[2], ARGV[1])

-- DB 영속화
-- KEYS[2]: Dirty 필래그 키 (예: ranking:dirty:2026-01-24)
-- ARGV[1]: 유저 ID
redis.call('SADD', KEYS[2], ARGV[1])

return 1