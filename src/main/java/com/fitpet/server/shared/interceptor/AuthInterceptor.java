package com.fitpet.server.shared.interceptor;

import com.fitpet.server.security.jwt.JwtTokenProvider;
import com.fitpet.server.shared.metrics.UserActivityMetrics;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserActivityMetrics userActivityMetrics;
    public static final String DEV_HEADER = "dev-user-id";

    @Value("${spring.profiles.active:prod}")
    private String activeProfile;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        if (isDevEnvironment()) {
            String devUserId = request.getHeader(DEV_HEADER);
            if (StringUtils.hasText(devUserId)) {
                try {
                    Long userId = Long.parseLong(devUserId);
                    request.setAttribute("userId", userId);
                    userActivityMetrics.recordActiveUser(userId);
                    return true;
                } catch (NumberFormatException e) {
                    log.warn("Invalid dev-user-id header value: {}", devUserId);
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid dev-user-id format");
                    return false;
                }
            }
        }

        String token = resolveToken(request);

        if (token == null || !jwtTokenProvider.validateAccessToken(token)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        Long userId = jwtTokenProvider.getUserId(token, false);
        request.setAttribute("userId", userId);
        userActivityMetrics.recordActiveUser(userId);

        return true;
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private boolean isDevEnvironment() {
        return "dev".equals(activeProfile) || "local".equals(activeProfile) || "test".equals(activeProfile);
    }
}
