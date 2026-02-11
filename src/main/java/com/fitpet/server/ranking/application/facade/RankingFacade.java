package com.fitpet.server.ranking.application.facade;

import com.fitpet.server.ranking.application.dto.RankingDto;
import com.fitpet.server.ranking.application.dto.RankingSummaryDto;
import com.fitpet.server.ranking.application.service.RankingService;
import com.fitpet.server.ranking.domain.type.RankingFilter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RankingFacade {

    private final RankingService rankingService;

    public RankingSummaryDto getRankingSummary(Long userId, RankingFilter filter) {
        List<RankingDto> top10Dtos = rankingService.getTop10(filter);

        RankingDto myRankDto = rankingService.getMyRank(userId, filter);

        return RankingSummaryDto.of(top10Dtos, myRankDto);
    }

    public void updateScore(Long userId, int steps) {
        rankingService.updateScore(userId, steps);
    }
}