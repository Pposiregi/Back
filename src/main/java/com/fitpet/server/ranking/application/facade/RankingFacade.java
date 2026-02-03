package com.fitpet.server.ranking.application.facade;

import com.fitpet.server.ranking.application.dto.RankingDto;
import com.fitpet.server.ranking.application.service.RankingService;
import com.fitpet.server.ranking.presentation.dto.RankingResponse;
import com.fitpet.server.ranking.presentation.dto.RankingSummaryResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RankingFacade {

    private final RankingService rankingService;

    public RankingSummaryResponse getRankingSummary(Long userId) {
        List<RankingDto> top10Dtos = rankingService.getTop10();
        RankingDto myRankDto = rankingService.getMyRank(userId);

        List<RankingResponse> top10Responses = top10Dtos.stream()
                .map(RankingResponse::from)
                .toList();

        RankingResponse myRankResponse = RankingResponse.from(myRankDto);

        return RankingSummaryResponse.of(top10Responses, myRankResponse);
    }

    public void updateScore(Long userId, int steps) {
        rankingService.updateScore(userId, steps);
    }
}