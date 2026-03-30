package com.fitpet.server.auth.application.dto;

import com.fitpet.server.auth.domain.entity.type.AuthEventType;
import com.fitpet.server.auth.domain.entity.type.AuthProvider;

public record CreateAuthLogCommand(
        Long userId,
        String attemptedEmail,
        AuthEventType eventType,
        AuthProvider provider,
        String ipAddress,
        String userAgent,
        boolean success
) {}
