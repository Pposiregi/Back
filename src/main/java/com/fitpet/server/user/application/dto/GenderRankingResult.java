package com.fitpet.server.user.application.dto;

import java.util.List;

public record GenderRankingResult(
        List<UserRanking> top10
) {
}
