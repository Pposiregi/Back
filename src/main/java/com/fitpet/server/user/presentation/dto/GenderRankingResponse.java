package com.fitpet.server.user.presentation.dto;

import java.util.List;

import com.fitpet.server.user.application.dto.GenderRankingResult;

public record GenderRankingResponse(
        List<UserRankingDto> top10
) {

    public static GenderRankingResponse from(GenderRankingResult result) {
        return new GenderRankingResponse(
                result.top10().stream()
                    .map(UserRankingDto::from)
                    .toList()
        );
    }
}
