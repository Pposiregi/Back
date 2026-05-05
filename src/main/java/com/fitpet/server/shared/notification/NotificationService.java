package com.fitpet.server.shared.notification;

public interface NotificationService {
    void notifyError(Throwable throwable, ErrorContext context);
    void notifySignup(Long userId, long totalCount);
}
