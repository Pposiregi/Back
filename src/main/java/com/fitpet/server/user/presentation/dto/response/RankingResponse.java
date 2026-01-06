package com.fitpet.server.user.presentation.dto.response;

import com.fitpet.server.user.application.dto.RankingResult;
import com.fitpet.server.user.presentation.dto.UserRankingDto;
import java.util.List;

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
