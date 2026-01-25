package com.fitpet.server.ranking.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankingResponse {
    private int rank;
    private Long userId;
    private String nickname;
    private Long score;

    public static RankingResponse of(Long userId, String nickname, int rank, long score) {
        return RankingResponse.builder()
                .userId(userId)
                .nickname(nickname)
                .rank(rank)
                .score(score)
                .build();
    }
}