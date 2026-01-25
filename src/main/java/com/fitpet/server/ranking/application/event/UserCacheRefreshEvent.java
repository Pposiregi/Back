package com.fitpet.server.ranking.application.event;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserCacheRefreshEvent {
    private final List<Long> userIds; // 캐시가 비어있는 유저 ID 목록
}
