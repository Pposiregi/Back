package com.fitpet.server.shared.notification;

import jakarta.servlet.http.HttpServletRequest;

public record ErrorContext(String uri, String method, Long userId) {

    public static ErrorContext from(HttpServletRequest request) {
        return new ErrorContext(request.getRequestURI(), request.getMethod(), null);
    }
}
