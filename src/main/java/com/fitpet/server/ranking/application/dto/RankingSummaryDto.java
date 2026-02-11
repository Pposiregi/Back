package com.fitpet.server.ranking.application.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RankingSummaryDto {
    private List<RankingDto> topRankings;
    private RankingDto myRanking;

    public static RankingSummaryDto of(List<RankingDto> topRankings, RankingDto myRanking) {
        return RankingSummaryDto.builder()
                .topRankings(topRankings)
                .myRanking(myRanking)
                .build();
    }
}