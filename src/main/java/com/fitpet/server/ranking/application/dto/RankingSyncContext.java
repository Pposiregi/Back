package com.fitpet.server.ranking.application.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RankingSyncContext {
    private final String dateKey;
    private final String redisKey;
    private final String dirtyKey;

    public static RankingSyncContext of(String dateKey, String redisKey, String dirtyKey) {
        return new RankingSyncContext(dateKey, redisKey, dirtyKey);
    }
}