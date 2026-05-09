package com.fitpet.server.ranking.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OvertakeMessage {

    private Long outboxId;

    private Long overtakenUserId;

    private Long overtakingUserId;

    private String overtakingUserNickname;

    private String dateKey;

    private int overtakerCount;
}
