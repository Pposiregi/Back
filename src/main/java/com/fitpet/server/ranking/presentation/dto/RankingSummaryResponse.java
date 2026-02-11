package com.fitpet.server.ranking.presentation.dto;

import com.fitpet.server.ranking.application.dto.RankingSummaryDto;
import java.util.List;
import java.util.stream.Collectors;
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

    public static RankingSummaryResponse from(RankingSummaryDto dto) {
        return RankingSummaryResponse.builder()
                .topRankings(dto.getTopRankings().stream()
                        .map(RankingResponse::from)
                        .collect(Collectors.toList()))
                .myRanking(RankingResponse.from(dto.getMyRanking()))
                .build();
    }
}