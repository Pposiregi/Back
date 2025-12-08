package com.fitpet.server.user.application.dto;

public record UserRanking(
        Long userId,
        String nickname,
        Integer dailyStepCount
) {
}
