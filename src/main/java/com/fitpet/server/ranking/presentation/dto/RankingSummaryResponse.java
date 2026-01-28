package com.fitpet.server.ranking.presentation.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RankingSummaryResponse {
    private List<RankingResponse> topRankings;
    private RankingResponse myRanking;

    public static RankingSummaryResponse of(List<RankingResponse> topRankings, RankingResponse myRanking) {
        return RankingSummaryResponse.builder()
                .topRankings(topRankings)
                .myRanking(myRanking)
                .build();
    }
}