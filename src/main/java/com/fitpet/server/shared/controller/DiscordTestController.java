package com.fitpet.server.shared.controller;

import com.fitpet.server.shared.notification.ErrorContext;
import com.fitpet.server.shared.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/test/discord")
public class DiscordTestController {

    private final NotificationService notificationService;

    @PostMapping("/error")
    public String testError() {
        notificationService.notifyError(
                new RuntimeException("Discord 에러 훅 테스트"),
                new ErrorContext("/api/test/discord/error", "POST", null, null)
        );
        return "error notification sent";
    }

    @PostMapping("/signup")
    public String testSignup() {
        notificationService.notifySignup(9999L, 9999L);
        return "signup notification sent";
    }
}
