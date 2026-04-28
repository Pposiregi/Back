package com.fitpet.server.auth.application.service;

import com.fitpet.server.auth.presentation.dto.LoginRequest;
import com.fitpet.server.auth.presentation.dto.TokenResponse;

public interface AuthService {
    TokenResponse login(LoginRequest request);

    TokenResponse refresh(String refreshToken);

    void logout(String accessToken);

    TokenResponse loginWithGoogle(String idToken);

    TokenResponse loginWithKakao(String kakaoAccessToken);

    void revokeTokens(Long userId);
}