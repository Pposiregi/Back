package com.fitpet.server.auth.application.service;

import com.fitpet.server.auth.domain.exception.InvalidAccessTokenException;
import com.fitpet.server.auth.domain.exception.InvalidLoginException;
import com.fitpet.server.auth.domain.exception.InvalidRefreshTokenException;
import com.fitpet.server.auth.infra.GoogleTokenVerifier;
import com.fitpet.server.auth.infra.KakaoClient;
import com.fitpet.server.auth.infra.KakaoClient.KakaoProfile;
import com.fitpet.server.auth.infra.RedisTokenRepository;
import com.fitpet.server.auth.presentation.dto.GoogleProfile;
import com.fitpet.server.auth.presentation.dto.LoginRequest;
import com.fitpet.server.auth.presentation.dto.TokenResponse;
import com.fitpet.server.security.jwt.JwtTokenProvider;
import com.fitpet.server.user.domain.entity.RegistrationStatus;
import com.fitpet.server.user.domain.entity.User;
import com.fitpet.server.user.domain.exception.UserNotFoundException;
import com.fitpet.server.user.domain.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTokenRepository redisTokenRepository;
    private final KakaoClient kakaoClient;
    private final GoogleTokenVerifier googleTokenVerifier;

    @Override
    @Transactional
    public TokenResponse login(LoginRequest request) {
        log.info("[AuthService] 로그인 시도");
        User user = userRepository.findByEmail(request.email()).orElse(null);

        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidLoginException();
        }

        return issueTokens(user);
    }

    @Override
    public TokenResponse refresh(String refreshToken) {
        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw new InvalidRefreshTokenException();
        }

        Long userId = jwtTokenProvider.getUserId(refreshToken, true);

        String saved = redisTokenRepository.find(userId);
        if (saved == null || !saved.equals(refreshToken)) {
            throw new InvalidRefreshTokenException();
        }

        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        return issueTokens(user);
    }

    @Override
    public void logout(String accessToken) {
        log.info("[AuthService] 로그아웃 시도");

        if (!jwtTokenProvider.validateAccessToken(accessToken)) {
            throw new InvalidAccessTokenException();
        }

        Long userId = jwtTokenProvider.getUserId(accessToken, false);

        redisTokenRepository.delete(userId);
    }

    @Override
    public TokenResponse loginWithGoogle(String idToken) {
        GoogleProfile profile = googleTokenVerifier.verifyIdToken(idToken);
        String email = profile.email();
        String providerUid = profile.sub();

        User user = upsertOAuthUser(email, "GOOGLE", providerUid);
        return issueTokens(user);
    }

    @Override
    public TokenResponse loginWithKakao(String kakaoAccessToken) {
        KakaoProfile profile = kakaoClient.getProfile(kakaoAccessToken);
        String email = (profile.email() == null || profile.email().isBlank())
            ? ("kakao_" + profile.id() + "@anon.kakao.local")
            : profile.email();

        User user = upsertOAuthUser(email, "KAKAO", profile.id());
        return issueTokens(user);
    }

    private User upsertOAuthUser(String email, String provider, String providerUid) {
        // 1) provider+uid로 조회 (탈퇴 계정 포함)
        Optional<User> byProvider = userRepository.findByOAuthIncludeDeleted(provider, providerUid);
        if (byProvider.isPresent()) {
            User user = byProvider.get();
            if (user.getDeletedAt() != null) {
                user.reactivate();
                return userRepository.save(user);
            }
            return user;
        }

        // 2) 이메일로 기존 계정이 있으면 연결 (탈퇴 계정 포함)
        if (email != null && !email.isBlank()) {
            Optional<User> byEmail = userRepository.findByEmailIncludeDeleted(email);
            if (byEmail.isPresent()) {
                User user = byEmail.get();
                user.linkSocial(provider, providerUid);
                if (user.getDeletedAt() != null) {
                    user.reactivate();
                }
                return userRepository.save(user);
            }
        }

        // 3) 신규 생성
        String dummyPw = passwordEncoder.encode(UUID.randomUUID().toString());
        User user = User.builder()
            .email(email != null ? email : ("anon+" + provider + "-" + providerUid + "@local"))
            .password(dummyPw)
            .nickname((email != null && email.contains("@")) ? email.substring(0, email.indexOf('@'))
                : (provider.toLowerCase() + "_" + providerUid))
            .provider(provider)
            .providerUid(providerUid)
            .registrationStatus(RegistrationStatus.INCOMPLETE)
            .build();
        return userRepository.save(user);
    }

    private TokenResponse issueTokens(User user) {
        String access = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), List.of("ROLE_USER"));
        String refresh = jwtTokenProvider.generateRefreshToken(user.getId(), user.getEmail());
        redisTokenRepository.save(user.getId(), refresh, jwtTokenProvider.getRefreshExpirationMs());
        return TokenResponse.success(resolveRegistrationStatus(user), access, refresh);
    }

    @Override
    public void revokeTokens(Long userId) {
        redisTokenRepository.delete(userId);
    }

    private RegistrationStatus resolveRegistrationStatus(User user) {
        RegistrationStatus status = user.getRegistrationStatus();
        return status != null ? status : RegistrationStatus.INCOMPLETE;
    }
}