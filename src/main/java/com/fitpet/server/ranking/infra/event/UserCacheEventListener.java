package com.fitpet.server.ranking.infra.event;

import com.fitpet.server.ranking.application.event.UserCacheRefreshEvent;
import com.fitpet.server.user.domain.entity.User;
import com.fitpet.server.user.domain.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserCacheEventListener {
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;

    @Async
    @EventListener
    public void handleCacheRefresh(UserCacheRefreshEvent event) {
        List<User> users = userRepository.findAllById(event.getUserIds());
        Map<String, String> map = users.stream()
                .collect(Collectors.toMap(u -> String.valueOf(u.getId()), User::getNickname));
        redisTemplate.opsForHash().putAll("user:profiles", map);
    }
}
