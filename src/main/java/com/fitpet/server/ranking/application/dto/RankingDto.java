package com.fitpet.server.ranking.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RankingDto {
    private int rank;
    private Long userId;
    private double score;
}
