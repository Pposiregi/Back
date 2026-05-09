-- user:profiles Hash에 닉네임을 저장하고 TTL을 원자적으로 설정
-- KEYS[1]: hash key (예: user:profiles)
-- ARGV[1]: field (userId)
-- ARGV[2]: value (nickname)
-- ARGV[3]: TTL (초 단위)
redis.call('HSET', KEYS[1], ARGV[1], ARGV[2])
redis.call('EXPIRE', KEYS[1], tonumber(ARGV[3]))
return 1
