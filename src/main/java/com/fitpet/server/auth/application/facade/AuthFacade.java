package com.fitpet.server.auth.application.facade;

import com.fitpet.server.auth.application.dto.CreateAuthLogCommand;
import com.fitpet.server.auth.application.service.AuthLogService;
import com.fitpet.server.auth.application.service.AuthService;
import com.fitpet.server.auth.domain.entity.type.AuthEventType;
import com.fitpet.server.auth.domain.entity.type.AuthProvider;
import com.fitpet.server.auth.presentation.dto.LoginRequest;
import com.fitpet.server.auth.presentation.dto.TokenResponse;
import com.fitpet.server.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthFacade {

    private final AuthService authService;
    private final AuthLogService authLogService;
    private final JwtTokenProvider jwtTokenProvider;

    public TokenResponse login(LoginRequest request, String ip, String ua) {
        try {
            TokenResponse result = authService.login(request);
            recordLogin(result, request.email(), AuthProvider.LOCAL, ip, ua, true);
            return result;
        } catch (Exception e) {
            recordLogin(null, request.email(), AuthProvider.LOCAL, ip, ua, false);
            throw e;
        }
    }

    public TokenResponse loginWithGoogle(String idToken, String ip, String ua) {
        try {
            TokenResponse result = authService.loginWithGoogle(idToken);
            recordLogin(result, null, AuthProvider.GOOGLE, ip, ua, true);
            return result;
        } catch (Exception e) {
            recordLogin(null, null, AuthProvider.GOOGLE, ip, ua, false);
            throw e;
        }
    }

    public TokenResponse loginWithKakao(String kakaoToken, String ip, String ua) {
        try {
            TokenResponse result = authService.loginWithKakao(kakaoToken);
            recordLogin(result, null, AuthProvider.KAKAO, ip, ua, true);
            return result;
        } catch (Exception e) {
            recordLogin(null, null, AuthProvider.KAKAO, ip, ua, false);
            throw e;
        }
    }

    public void logout(String accessToken, String ip, String ua) {
        Long userId = null;
        try {
            userId = extractUserId(accessToken);
            authService.logout(accessToken);
            recordLogout(userId, ip, ua, true);
        } catch (Exception e) {
            recordLogout(userId, ip, ua, false);
            throw e;
        }
    }

    public TokenResponse refresh(String refreshToken) {
        return authService.refresh(refreshToken);
    }

    private void recordLogin(TokenResponse result, String email,
                             AuthProvider provider, String ip, String ua, boolean success) {
        try {
            Long userId = success ? extractUserIdFromResult(result) : null;
            authLogService.record(new CreateAuthLogCommand(
                    userId, email, AuthEventType.LOGIN, provider, ip, ua, success));
        } catch (Exception e) {
            log.error("[AuthFacade] 로그인 로그 기록 실패 (무시): provider={}, success={}", provider, success, e);
        }
    }

    private void recordLogout(Long userId, String ip, String ua, boolean success) {
        authLogService.record(new CreateAuthLogCommand(
                userId, null, AuthEventType.LOGOUT, AuthProvider.LOCAL, ip, ua, success));
    }

    private Long extractUserIdFromResult(TokenResponse result) {
        return jwtTokenProvider.getUserId(result.serverRefreshToken(), true);
    }

    private Long extractUserId(String accessToken) {
        return jwtTokenProvider.getUserId(accessToken, false);
    }
}
