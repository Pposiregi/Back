package com.fitpet.server.auth.application.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fitpet.server.auth.application.dto.CreateAuthLogCommand;
import com.fitpet.server.auth.application.service.AuthLogService;
import com.fitpet.server.auth.application.service.AuthService;
import com.fitpet.server.auth.domain.entity.type.AuthEventType;
import com.fitpet.server.auth.domain.entity.type.AuthProvider;
import com.fitpet.server.auth.domain.exception.InvalidLoginException;
import com.fitpet.server.auth.domain.exception.OAuthInvalidTokenException;
import com.fitpet.server.auth.presentation.dto.LoginRequest;
import com.fitpet.server.auth.presentation.dto.TokenResponse;
import com.fitpet.server.security.jwt.JwtTokenProvider;
import com.fitpet.server.user.domain.entity.RegistrationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthFacadeTest {

    @Mock AuthService authService;
    @Mock AuthLogService authLogService;
    @Mock JwtTokenProvider jwtTokenProvider;
    @InjectMocks AuthFacade sut;

    @Test
    void 로그인_성공시_success_true_로그가_기록된다() {
        LoginRequest req = new LoginRequest("user@test.com", "pw");
        TokenResponse token = TokenResponse.success(RegistrationStatus.COMPLETE, "access", "refresh");
        when(authService.login(req)).thenReturn(token);
        when(jwtTokenProvider.getUserId("refresh", true)).thenReturn(1L);

        sut.login(req, "1.1.1.1", "ua");

        ArgumentCaptor<CreateAuthLogCommand> captor = ArgumentCaptor.forClass(CreateAuthLogCommand.class);
        verify(authLogService).record(captor.capture());
        CreateAuthLogCommand logged = captor.getValue();
        assertThat(logged.success()).isTrue();
        assertThat(logged.userId()).isEqualTo(1L);
        assertThat(logged.attemptedEmail()).isEqualTo("user@test.com");
        assertThat(logged.provider()).isEqualTo(AuthProvider.LOCAL);
        assertThat(logged.eventType()).isEqualTo(AuthEventType.LOGIN);
    }

    @Test
    void 로그인_실패시_success_false_로그가_기록되고_예외가_전파된다() {
        LoginRequest req = new LoginRequest("bad@test.com", "wrong");
        when(authService.login(req)).thenThrow(new InvalidLoginException());

        assertThatThrownBy(() -> sut.login(req, "1.1.1.1", "ua"))
                .isInstanceOf(InvalidLoginException.class);

        ArgumentCaptor<CreateAuthLogCommand> captor = ArgumentCaptor.forClass(CreateAuthLogCommand.class);
        verify(authLogService).record(captor.capture());
        CreateAuthLogCommand logged = captor.getValue();
        assertThat(logged.success()).isFalse();
        assertThat(logged.userId()).isNull();
        assertThat(logged.attemptedEmail()).isEqualTo("bad@test.com");
    }

    @Test
    void 로그아웃_성공시_LOGOUT_로그가_기록된다() {
        when(jwtTokenProvider.getUserId("token", false)).thenReturn(1L);

        sut.logout("token", "2.2.2.2", "ua");

        ArgumentCaptor<CreateAuthLogCommand> captor = ArgumentCaptor.forClass(CreateAuthLogCommand.class);
        verify(authLogService).record(captor.capture());
        CreateAuthLogCommand logged = captor.getValue();
        assertThat(logged.eventType()).isEqualTo(AuthEventType.LOGOUT);
        assertThat(logged.userId()).isEqualTo(1L);
        assertThat(logged.success()).isTrue();
    }

    @Test
    void 구글_로그인_성공시_GOOGLE_provider_success_true_로그가_기록된다() {
        TokenResponse token = TokenResponse.success(RegistrationStatus.COMPLETE, "access", "refresh");
        when(authService.loginWithGoogle("id-token")).thenReturn(token);
        when(jwtTokenProvider.getUserId("refresh", true)).thenReturn(2L);

        sut.loginWithGoogle("id-token", "3.3.3.3", "ua");

        ArgumentCaptor<CreateAuthLogCommand> captor = ArgumentCaptor.forClass(CreateAuthLogCommand.class);
        verify(authLogService).record(captor.capture());
        CreateAuthLogCommand logged = captor.getValue();
        assertThat(logged.success()).isTrue();
        assertThat(logged.userId()).isEqualTo(2L);
        assertThat(logged.provider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(logged.eventType()).isEqualTo(AuthEventType.LOGIN);
        assertThat(logged.attemptedEmail()).isNull();
    }

    @Test
    void 구글_로그인_실패시_GOOGLE_provider_success_false_로그가_기록되고_예외가_전파된다() {
        when(authService.loginWithGoogle("bad-token")).thenThrow(new OAuthInvalidTokenException());

        assertThatThrownBy(() -> sut.loginWithGoogle("bad-token", "3.3.3.3", "ua"))
                .isInstanceOf(OAuthInvalidTokenException.class);

        ArgumentCaptor<CreateAuthLogCommand> captor = ArgumentCaptor.forClass(CreateAuthLogCommand.class);
        verify(authLogService).record(captor.capture());
        CreateAuthLogCommand logged = captor.getValue();
        assertThat(logged.success()).isFalse();
        assertThat(logged.userId()).isNull();
        assertThat(logged.provider()).isEqualTo(AuthProvider.GOOGLE);
    }

    @Test
    void 카카오_로그인_성공시_KAKAO_provider_success_true_로그가_기록된다() {
        TokenResponse token = TokenResponse.success(RegistrationStatus.INCOMPLETE, "access", "refresh");
        when(authService.loginWithKakao("kakao-token")).thenReturn(token);
        when(jwtTokenProvider.getUserId("refresh", true)).thenReturn(3L);

        sut.loginWithKakao("kakao-token", "4.4.4.4", "ua");

        ArgumentCaptor<CreateAuthLogCommand> captor = ArgumentCaptor.forClass(CreateAuthLogCommand.class);
        verify(authLogService).record(captor.capture());
        CreateAuthLogCommand logged = captor.getValue();
        assertThat(logged.success()).isTrue();
        assertThat(logged.userId()).isEqualTo(3L);
        assertThat(logged.provider()).isEqualTo(AuthProvider.KAKAO);
        assertThat(logged.eventType()).isEqualTo(AuthEventType.LOGIN);
        assertThat(logged.attemptedEmail()).isNull();
    }

    @Test
    void 카카오_로그인_실패시_KAKAO_provider_success_false_로그가_기록되고_예외가_전파된다() {
        when(authService.loginWithKakao("bad-token")).thenThrow(new OAuthInvalidTokenException());

        assertThatThrownBy(() -> sut.loginWithKakao("bad-token", "4.4.4.4", "ua"))
                .isInstanceOf(OAuthInvalidTokenException.class);

        ArgumentCaptor<CreateAuthLogCommand> captor = ArgumentCaptor.forClass(CreateAuthLogCommand.class);
        verify(authLogService).record(captor.capture());
        CreateAuthLogCommand logged = captor.getValue();
        assertThat(logged.success()).isFalse();
        assertThat(logged.userId()).isNull();
        assertThat(logged.provider()).isEqualTo(AuthProvider.KAKAO);
    }
}
