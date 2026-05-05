package com.fitpet.server.user.application.event;

import com.fitpet.server.shared.notification.NotificationService;
import com.fitpet.server.user.domain.event.SignupCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class SignupNotificationListener {

    private final NotificationService notificationService;

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handle(SignupCompletedEvent event) {
        notificationService.notifySignup(event.userId(), event.totalCount());
    }
}
