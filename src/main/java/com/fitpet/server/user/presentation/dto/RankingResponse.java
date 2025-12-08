package com.fitpet.server.user.presentation.dto;

import java.util.List;

import com.fitpet.server.user.application.dto.RankingResult;

public record RankingResponse(
        List<UserRankingDto> top10,
        int myRank
) {

    public static RankingResponse from(RankingResult result) {
        return new RankingResponse(
            result.top10().stream()
                .map(UserRankingDto::from)
                .toList(),
            result.myRank()
        );
    }
}
