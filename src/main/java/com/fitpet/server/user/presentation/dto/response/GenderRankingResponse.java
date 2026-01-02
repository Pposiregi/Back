package com.fitpet.server.user.presentation.dto.response;

import com.fitpet.server.user.application.dto.GenderRankingResult;
import com.fitpet.server.user.presentation.dto.UserRankingDto;
import java.util.List;

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
