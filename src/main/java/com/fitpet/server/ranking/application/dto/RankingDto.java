package com.fitpet.server.ranking.application.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RankingDto {
    private Long userId;
    private String nickname;
    private int rank;
    private long score;

    public static RankingDto of(Long userId, String nickname, int rank, long score) {
        return RankingDto.builder()
                .userId(userId)
                .nickname(nickname)
                .rank(rank)
                .score(score)
                .build();
    }
}