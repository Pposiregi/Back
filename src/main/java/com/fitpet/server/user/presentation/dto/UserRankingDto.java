package com.fitpet.server.user.presentation.dto;

import com.fitpet.server.user.application.dto.UserRanking;

public record UserRankingDto(
        Long userId,
        String nickname,
        Integer dailyStepCount
) {

    public static UserRankingDto from(UserRanking ranking) {
        return new UserRankingDto(
                ranking.userId(),
                ranking.nickname(),
                ranking.dailyStepCount()
        );
    }
}
