package com.fitpet.server.user.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserTest {

    // ──────────────────────────────────────────────
    // withdraw()
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("withdraw 호출 시 deletedAt이 설정된다")
    void withdraw_deletedAt_설정() {
        User user = User.builder().id(1L).email("test@test.com").build();

        user.withdraw();

        assertThat(user.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("withdraw 호출 시 profileImageUrl과 deviceToken이 null이 된다")
    void withdraw_profileImageUrl_deviceToken_null화() {
        User user = User.builder()
                .id(1L)
                .email("test@test.com")
                .profileImageUrl("s3://bucket/profile.jpg")
                .deviceToken("fcm-token-abc")
                .build();

        user.withdraw();

        assertThat(user.getProfileImageUrl()).isNull();
        assertThat(user.getDeviceToken()).isNull();
    }

    @Test
    @DisplayName("withdraw 호출 후 email, nickname 등 다른 필드는 변경되지 않는다")
    void withdraw_다른_필드는_유지() {
        User user = User.builder()
                .id(1L)
                .email("test@test.com")
                .nickname("테스트닉네임")
                .registrationStatus(RegistrationStatus.COMPLETE)
                .build();

        user.withdraw();

        assertThat(user.getEmail()).isEqualTo("test@test.com");
        assertThat(user.getNickname()).isEqualTo("테스트닉네임");
        assertThat(user.getRegistrationStatus()).isEqualTo(RegistrationStatus.COMPLETE);
    }

    // ──────────────────────────────────────────────
    // reactivate()
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("reactivate 호출 시 deletedAt이 null로 복원된다")
    void reactivate_deletedAt_null_복원() {
        User user = User.builder().id(1L).email("test@test.com").build();
        user.withdraw();
        assertThat(user.getDeletedAt()).isNotNull();

        user.reactivate();

        assertThat(user.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("withdraw 후 reactivate 연속 호출 시 deletedAt이 null로 복원된다")
    void withdraw_후_reactivate_정상_복원() {
        User user = User.builder()
                .id(1L)
                .email("test@test.com")
                .nickname("닉네임")
                .build();

        user.withdraw();
        user.reactivate();

        assertThat(user.getDeletedAt()).isNull();
        assertThat(user.getEmail()).isEqualTo("test@test.com");
        assertThat(user.getNickname()).isEqualTo("닉네임");
    }
}
