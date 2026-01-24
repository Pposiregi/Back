-- 랭킹 업데이트
redis.call('ZADD', KEYS[1], ARGV[2], ARGV[1])

-- DB 영속화
redis.call('SADD', KEYS[2], ARGV[1])

return 1