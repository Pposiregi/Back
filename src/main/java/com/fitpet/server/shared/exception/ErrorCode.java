package com.fitpet.server.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {


    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "U002", "이미 존재하는 이메일입니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "U003", "이미 존재하는 닉네임입니다."),

    DAILY_WALK_NOT_FOUND(HttpStatus.NOT_FOUND, "D001", "해당 걸음 기록을 찾을 수 없습니다."),
    DAILY_WALK_ALREADY_EXISTS(HttpStatus.CONFLICT, "D002", "해당 날짜의 걸음 기록이 이미 존재합니다."),

    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "W001", "해당 운동 세션을 찾을 수 없습니다."),

    BODY_HISTORY_NOT_FOUND(HttpStatus.NOT_FOUND, "B001", "해당 신체 기록을 찾을 수 없습니다."),
    BODY_HISTORY_ALREADY_EXISTS(HttpStatus.CONFLICT, "B002", "해당 날짜의 신체 기록이 이미 존재합니다."),

    MISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "MI001", "해당 미션을 찾을 수 없습니다."),
    MISSION_ALREADY_EXISTS(HttpStatus.CONFLICT, "MI002", "해당 날짜의 미션이 이미 존재합니다."),
    MISSION_CHECK_NOT_FOUND(HttpStatus.NOT_FOUND, "MI003", "해당 미션 수행 기록을 찾을 수 없습니다."),

    BADGE_NOT_FOUND(HttpStatus.NOT_FOUND, "BA001", "해당 뱃지를 찾을 수 없습니다."),
    BADGE_CHECK_NOT_FOUND(HttpStatus.NOT_FOUND, "BA002", "해당 뱃지 획득 기록을 찾을 수 없습니다."),

    PET_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "사용자의 펫을 찾을 수 없습니다."),
    PET_ALREADY_EXISTS(HttpStatus.CONFLICT, "P002", "사용자는 이미 펫을 보유하고 있습니다."),
    PET_ACCESS_DENIED(HttpStatus.FORBIDDEN, "P003", "해당 펫에 대한 권한이 없습니다."),


    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "C001", "잘못된 요청입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C999", "서버 에러가 발생했습니다."),

    MEAL_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "해당 식단 기록을 찾을 수 없습니다."),
    MEAL_ACCESS_DENIED(HttpStatus.FORBIDDEN, "M002", "해당 식단 기록에 대한 권한이 없습니다."),

    DEVICE_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "F001", "사용자의 디바이스 토큰이 없습니다."),
    FCM_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "F002", "FCM 알림 발송에 실패했습니다."),

    INVALID_LOGIN(HttpStatus.UNAUTHORIZED, "A001", "아이디 또는 비밀번호가 일치하지 않습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "A002", "유효하지 않은 리프레시 토큰입니다."),
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "A003", "유효하지 않은 액세스 토큰입니다."),
    OAUTH_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A004", "유효하지 않은 소셜 토큰입니다."),
    OAUTH_VERIFICATION_FAILED(HttpStatus.BAD_GATEWAY, "A005", "소셜 토큰 검증에 실패했습니다."),
    OAUTH_PROFILE_NOT_FOUND(HttpStatus.BAD_GATEWAY, "A006", "소셜 사용자 정보를 확인할 수 없습니다.");


    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
