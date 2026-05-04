package com.fitpet.server.ranking.application.event;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RankingScoreUpdatedEvent {
    private final Long userId;
    private final Long previousRank;
    private final LocalDate date;
}
