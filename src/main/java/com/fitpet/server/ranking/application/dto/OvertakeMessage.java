package com.fitpet.server.ranking.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * RabbitMQ로 발행하는 추월 알림 메시지 DTO.
 *
 * <p>JSON 직렬화를 위해 기본 생성자가 필요하다.</p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OvertakeMessage {

    /** 아웃박스 ID (중복 발송 추적용) */
    private Long outboxId;

    /** 추월 당한 사용자 (알림 수신자) */
    private Long overtakenUserId;

    /** 추월 한 사용자 */
    private Long overtakingUserId;

    /** 추월 한 사용자의 닉네임 */
    private String overtakingUserNickname;

    /** 랭킹 날짜 키 */
    private String dateKey;

    /** 이 이벤트에서 피추월자를 추월한 사람 수 */
    private int overtakerCount;
}
