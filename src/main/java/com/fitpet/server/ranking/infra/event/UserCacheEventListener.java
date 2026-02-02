package com.fitpet.server.ranking.infra.event;

import com.fitpet.server.ranking.application.event.UserCacheRefreshEvent;
import com.fitpet.server.user.domain.entity.User;
import com.fitpet.server.user.domain.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserCacheEventListener {
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;

    @Async
    @EventListener
    public void handleCacheRefresh(UserCacheRefreshEvent event) {
        try {
            List<User> users = userRepository.findAllById(event.getUserIds());

            if (users.isEmpty()) {
                return;
            }

            Map<String, String> map = users.stream()
                    .collect(Collectors.toMap(u -> String.valueOf(u.getId()), User::getNickname));
            redisTemplate.opsForHash().putAll("user:profiles", map);
            log.info("[CacheRefresh] 유저 {}명 캐시 갱신 완료", users.size());
        } catch (Exception e) {
            log.error("[CacheRefresh] 캐시 갱신 중 오류 발생! 대상 IDs: {}", event.getUserIds(), e);
        }

    }
}
