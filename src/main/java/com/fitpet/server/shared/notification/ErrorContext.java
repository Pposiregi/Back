package com.fitpet.server.shared.notification;

import com.fitpet.server.shared.security.UserDetailsImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public record ErrorContext(String uri, String method, Long userId, String queryString) {

    public static ErrorContext from(HttpServletRequest request) {
        return new ErrorContext(
                request.getRequestURI(),
                request.getMethod(),
                extractUserId(),
                request.getQueryString()
        );
    }

    private static Long extractUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl userDetails) {
            return userDetails.getUserId();
        }
        return null;
    }
}
