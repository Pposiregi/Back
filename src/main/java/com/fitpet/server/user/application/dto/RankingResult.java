package com.fitpet.server.user.application.dto;

import java.util.List;

public record RankingResult(
        List<UserRanking> top10,
        int myRank
) {
}
