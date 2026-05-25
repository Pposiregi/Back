package com.fitpet.server.user.domain.event;

public record SignupCompletedEvent(Long userId, long totalCount) {
}
