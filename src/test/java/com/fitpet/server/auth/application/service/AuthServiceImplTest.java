package com.fitpet.server.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fitpet.server.auth.domain.exception.OAuthProviderMismatchException;
import com.fitpet.server.auth.infra.GoogleTokenVerifier;
import com.fitpet.server.auth.infra.KakaoClient;
import com.fitpet.server.auth.infra.KakaoClient.KakaoProfile;
import com.fitpet.server.auth.infra.RedisTokenRepository;
import com.fitpet.server.auth.presentation.dto.GoogleProfile;
import com.fitpet.server.auth.presentation.dto.TokenResponse;
import com.fitpet.server.security.jwt.JwtTokenProvider;
import com.fitpet.server.user.domain.entity.RegistrationStatus;
import com.fitpet.server.user.domain.entity.User;
import com.fitpet.server.user.domain.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock RedisTokenRepository redisTokenRepository;
    @Mock KakaoClient kakaoClient;
    @Mock GoogleTokenVerifier googleTokenVerifier;

    @InjectMocks AuthServiceImpl sut;

    // loginWithGoogle — 탈퇴 계정 재활성화

    @Test
    @DisplayName("구글 로그인 시 provider+uid로 탈퇴 계정이 조회되면 reactivate 후 토큰 발급")
    void loginWithGoogle_탈퇴_계정_provider_uid로_재활성화() {
        // given
        User deleted = User.builder()
                .id(1L).email("google@test.com")
                .provider("GOOGLE").providerUid("google-sub-123")
                .registrationStatus(RegistrationStatus.COMPLETE)
                .build();
        deleted.withdraw();

        when(googleTokenVerifier.verifyIdToken("id-token"))
                .thenReturn(new GoogleProfile("google-sub-123", "google@test.com", true));
        when(userRepository.findByOAuthIncludeDeleted("GOOGLE", "google-sub-123"))
                .thenReturn(Optional.of(deleted));
        when(userRepository.save(deleted)).thenReturn(deleted);
        stubTokenIssue(deleted);

        // when
        TokenResponse result = sut.loginWithGoogle("id-token");

        // then
        assertThat(deleted.getDeletedAt()).isNull();
        verify(userRepository).save(deleted);
        assertThat(result.success()).isTrue();
    }

    @Test
    @DisplayName("구글 로그인 시 provider+uid 조회 결과가 활성 계정이면 reactivate 없이 토큰 발급")
    void loginWithGoogle_활성_계정이면_그대로_토큰_발급() {
        // given
        User active = User.builder()
                .id(2L).email("google@test.com")
                .provider("GOOGLE").providerUid("google-sub-456")
                .registrationStatus(RegistrationStatus.COMPLETE)
                .build();

        when(googleTokenVerifier.verifyIdToken("id-token"))
                .thenReturn(new GoogleProfile("google-sub-456", "google@test.com", true));
        when(userRepository.findByOAuthIncludeDeleted("GOOGLE", "google-sub-456"))
                .thenReturn(Optional.of(active));
        stubTokenIssue(active);

        // when
        sut.loginWithGoogle("id-token");

        // then
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("구글 로그인 시 이메일로 탈퇴 계정이 조회되면 소셜 연결 + reactivate 후 토큰 발급")
    void loginWithGoogle_탈퇴_계정_이메일로_소셜연결_후_재활성화() {
        // given
        User deleted = User.builder()
                .id(3L).email("local@test.com")
                .registrationStatus(RegistrationStatus.COMPLETE)
                .build();
        deleted.withdraw();

        when(googleTokenVerifier.verifyIdToken("id-token"))
                .thenReturn(new GoogleProfile("new-sub-789", "local@test.com", true));
        when(userRepository.findByOAuthIncludeDeleted("GOOGLE", "new-sub-789"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmailIncludeDeleted("local@test.com"))
                .thenReturn(Optional.of(deleted));
        when(userRepository.save(deleted)).thenReturn(deleted);
        stubTokenIssue(deleted);

        // when
        sut.loginWithGoogle("id-token");

        // then
        assertThat(deleted.getDeletedAt()).isNull();
        assertThat(deleted.getProvider()).isEqualTo("GOOGLE");
        assertThat(deleted.getProviderUid()).isEqualTo("new-sub-789");
        verify(userRepository).save(deleted);
    }

    @Test
    @DisplayName("구글 로그인 시 이메일 매칭 계정에 이미 다른 provider가 있으면 OAuthProviderMismatchException")
    void loginWithGoogle_이메일_매칭_계정에_다른_provider_있으면_예외() {
        User kakaoUser = User.builder()
                .id(5L).email("shared@test.com")
                .provider("KAKAO").providerUid("kakao-uid-777")
                .registrationStatus(RegistrationStatus.COMPLETE)
                .build();

        when(googleTokenVerifier.verifyIdToken("id-token"))
                .thenReturn(new GoogleProfile("google-sub-999", "shared@test.com", true));
        when(userRepository.findByOAuthIncludeDeleted("GOOGLE", "google-sub-999"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmailIncludeDeleted("shared@test.com"))
                .thenReturn(Optional.of(kakaoUser));

        assertThatThrownBy(() -> sut.loginWithGoogle("id-token"))
                .isInstanceOf(OAuthProviderMismatchException.class);
    }

    @Test
    @DisplayName("구글 로그인 시 탈퇴한 계정에 다른 provider가 있으면 OAuthProviderMismatchException")
    void loginWithGoogle_탈퇴_계정에_다른_provider_있으면_예외() {
        User deletedKakaoUser = User.builder()
                .id(6L).email("deleted@test.com")
                .provider("KAKAO").providerUid("kakao-uid-888")
                .registrationStatus(RegistrationStatus.COMPLETE)
                .build();
        deletedKakaoUser.withdraw();

        when(googleTokenVerifier.verifyIdToken("id-token"))
                .thenReturn(new GoogleProfile("google-sub-000", "deleted@test.com", true));
        when(userRepository.findByOAuthIncludeDeleted("GOOGLE", "google-sub-000"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmailIncludeDeleted("deleted@test.com"))
                .thenReturn(Optional.of(deletedKakaoUser));

        assertThatThrownBy(() -> sut.loginWithGoogle("id-token"))
                .isInstanceOf(OAuthProviderMismatchException.class);
    }

    @Test
    @DisplayName("구글 로그인 시 일치하는 계정이 없으면 신규 생성")
    void loginWithGoogle_일치_계정_없으면_신규_생성() {
        // given
        when(googleTokenVerifier.verifyIdToken("id-token"))
                .thenReturn(new GoogleProfile("brand-new-sub", "new@test.com", true));
        when(userRepository.findByOAuthIncludeDeleted("GOOGLE", "brand-new-sub"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmailIncludeDeleted("new@test.com"))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("dummy-pw");

        User newUser = User.builder().id(10L).email("new@test.com")
                .registrationStatus(RegistrationStatus.INCOMPLETE).build();
        when(userRepository.save(any(User.class))).thenReturn(newUser);
        stubTokenIssue(newUser);

        // when
        sut.loginWithGoogle("id-token");

        // then
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("new@test.com");
        assertThat(saved.getProvider()).isEqualTo("GOOGLE");
        assertThat(saved.getRegistrationStatus()).isEqualTo(RegistrationStatus.INCOMPLETE);
    }

    // loginWithKakao — 탈퇴 계정 재활성화

    @Test
    @DisplayName("카카오 로그인 시 provider+uid로 탈퇴 계정이 조회되면 reactivate 후 토큰 발급")
    void loginWithKakao_탈퇴_계정_재활성화() {
        // given
        User deleted = User.builder()
                .id(4L).email("kakao@test.com")
                .provider("KAKAO").providerUid("kakao-id-999")
                .registrationStatus(RegistrationStatus.COMPLETE)
                .build();
        deleted.withdraw();

        when(kakaoClient.getProfile("kakao-token"))
                .thenReturn(new KakaoProfile("kakao-id-999", "kakao@test.com"));
        when(userRepository.findByOAuthIncludeDeleted("KAKAO", "kakao-id-999"))
                .thenReturn(Optional.of(deleted));
        when(userRepository.save(deleted)).thenReturn(deleted);
        stubTokenIssue(deleted);

        // when
        sut.loginWithKakao("kakao-token");

        // then
        assertThat(deleted.getDeletedAt()).isNull();
        verify(userRepository).save(deleted);
    }

    @Test
    @DisplayName("카카오 로그인 시 이메일이 null이면 익명 이메일로 신규 생성")
    void loginWithKakao_이메일_null이면_익명_이메일_생성() {
        // given
        when(kakaoClient.getProfile("kakao-token"))
                .thenReturn(new KakaoProfile("kakao-id-000", null));
        when(userRepository.findByOAuthIncludeDeleted("KAKAO", "kakao-id-000"))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("dummy-pw");

        User newUser = User.builder().id(11L)
                .email("kakao_kakao-id-000@anon.kakao.local")
                .registrationStatus(RegistrationStatus.INCOMPLETE).build();
        when(userRepository.save(any(User.class))).thenReturn(newUser);
        stubTokenIssue(newUser);

        // when
        sut.loginWithKakao("kakao-token");

        // then
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).startsWith("kakao_");
        assertThat(captor.getValue().getEmail()).endsWith("@anon.kakao.local");
    }

    // helpers

    private void stubTokenIssue(User user) {
        when(jwtTokenProvider.generateAccessToken(any(), anyString(), any())).thenReturn("access");
        when(jwtTokenProvider.generateRefreshToken(any(), anyString())).thenReturn("refresh");
        when(jwtTokenProvider.getRefreshExpirationMs()).thenReturn(1_800_000L);
    }
}
