package com.fitpet.server.shared.interceptor;

import com.fitpet.server.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    public static final String DEV_HEADER = "dev-user-id";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String devUserId = request.getHeader(DEV_HEADER);
        if (StringUtils.hasText(devUserId)) {
            request.setAttribute("userId", Long.parseLong(devUserId));
            return true;
        }

        String token = resolveToken(request);

        if (token == null || !jwtTokenProvider.validateAccessToken(token)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        Long userId = jwtTokenProvider.getUserId(token, false);
        request.setAttribute("userId", userId);

        return true;
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}