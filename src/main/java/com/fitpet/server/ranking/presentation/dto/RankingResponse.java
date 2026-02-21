package com.fitpet.server.ranking.presentation.dto;

import com.fitpet.server.ranking.application.dto.RankingDto;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RankingResponse {
    private Long userId;
    private String nickname;
    private String profileImageUrl;
    private int score;
    private int rank;

    public static RankingResponse from(RankingDto dto) {
        return RankingResponse.builder()
                .userId(dto.getUserId())
                .nickname(dto.getNickname())
                .profileImageUrl(dto.getProfileImageUrl())
                .score((int) dto.getScore())
                .rank(dto.getRank())
                .build();
    }
}