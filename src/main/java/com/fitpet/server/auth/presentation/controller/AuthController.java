package com.fitpet.server.auth.presentation.controller;

import com.fitpet.server.auth.application.service.AuthService;
import com.fitpet.server.auth.presentation.dto.LoginRequest;
import com.fitpet.server.auth.presentation.dto.SocialLoginRequest;
import com.fitpet.server.auth.presentation.dto.TokenResponse;
import com.fitpet.server.security.jwt.JwtTokenProvider;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// AuthController
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    private ResponseEntity<TokenResponse> withRefreshCookie(TokenResponse tokens) {
        ResponseCookie cookie = ResponseCookie.from("REFRESH_TOKEN", tokens.serverRefreshToken())
            //TODO : 프론트 측 개발 끝난 후 secure true로 변경
            .httpOnly(true).secure(false)   // 운영할 때 반드시 true (HTTPS)
            .sameSite("None")              // 크로스 도메인일 때
            .path("/")
            .maxAge(Duration.ofMillis(jwtTokenProvider.getRefreshExpirationMs()))
            .build();

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .body(tokens.withoutRefreshToken());// refresh는 바디에 안 줌
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest request) {
        return withRefreshCookie(authService.login(request));
    }

    // Refresh 시 쿠키에서 읽음
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@CookieValue("REFRESH_TOKEN") String refreshToken) {
        return withRefreshCookie(authService.refresh(refreshToken));
    }

    // Logout 시 서버에서 Redis 삭제 + 쿠키 만료
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader
    ) {
        String accessToken = extractBearerToken(authHeader);
        if (accessToken == null) {
            return unauthorized();
        }

        authService.logout(accessToken);

        ResponseCookie expire = ResponseCookie.from("REFRESH_TOKEN", "")
            //TODO : 프론트 측 개발 끝난 후 secure true로 변경
            .httpOnly(true).secure(false)
            .sameSite("None").path("/")
            .maxAge(0)
            .build();

        return ResponseEntity.noContent()
            .header(HttpHeaders.SET_COOKIE, expire.toString())
            .build();
    }

    @PostMapping("/oauth/google")
    public ResponseEntity<TokenResponse> google(@RequestBody SocialLoginRequest req) {
        return withRefreshCookie(authService.loginWithGoogle(req.idToken()));
    }

    @PostMapping("/oauth/kakao")
    public ResponseEntity<TokenResponse> kakao(@RequestBody SocialLoginRequest req) {
        return withRefreshCookie(authService.loginWithKakao(req.accessToken()));
    }

    private static ResponseEntity<Void> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
            .build();
    }

    private static String extractBearerToken(String authHeader) {
        if (authHeader == null) {
            return null;
        }
        int space = authHeader.indexOf(' ');
        if (space < 0) {
            return null;
        }
        String scheme = authHeader.substring(0, space);
        if (!"Bearer".equalsIgnoreCase(scheme)) {
            return null;
        }
        String token = authHeader.substring(space + 1).trim();
        return token.isEmpty() ? null : token;
    }
}