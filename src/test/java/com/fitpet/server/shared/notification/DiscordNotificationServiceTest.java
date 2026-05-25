package com.fitpet.server.shared.notification;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fitpet.server.shared.config.DiscordWebhookProperties;
import com.fitpet.server.shared.notification.dto.DiscordWebhookPayload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DiscordNotificationServiceTest {

    @Mock DiscordWebhookClient webhookClient;
    @Mock DiscordWebhookProperties properties;
    @Mock GeminiErrorAnalyzer geminiErrorAnalyzer;
    @InjectMocks DiscordNotificationService sut;

    @Test
    @DisplayName("notifyError - enabled이면 에러 웹훅 URL로 Discord 메시지를 전송한다")
    void notifyError_enabled이면_에러_웹훅_전송() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getErrorWebhookUrl()).thenReturn("https://discord.test/error");

        sut.notifyError(new RuntimeException("test"), new ErrorContext("/api/test", "GET", null, null));

        verify(webhookClient).send(eq("https://discord.test/error"), any(DiscordWebhookPayload.class));
    }

    @Test
    @DisplayName("notifySignup - enabled이면 회원가입 웹훅 URL로 Discord 메시지를 전송한다")
    void notifySignup_enabled이면_회원가입_웹훅_전송() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getSignupWebhookUrl()).thenReturn("https://discord.test/signup");

        sut.notifySignup(42L, 100L);

        verify(webhookClient).send(eq("https://discord.test/signup"), any(DiscordWebhookPayload.class));
    }

    @Test
    @DisplayName("notifyError - disabled이면 웹훅을 전송하지 않는다")
    void notifyError_disabled이면_웹훅_미전송() {
        when(properties.isEnabled()).thenReturn(false);

        sut.notifyError(new RuntimeException("test"), new ErrorContext("/api/test", "GET", null, null));

        verify(webhookClient, never()).send(any(), any());
    }

    @Test
    @DisplayName("notifyError - 웹훅 전송 실패 시 예외를 삼키고 호출자에게 영향을 주지 않는다")
    void notifyError_웹훅_실패_시_예외_삼킴() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getErrorWebhookUrl()).thenReturn("https://discord.test/error");
        doThrow(new RuntimeException("webhook failed")).when(webhookClient).send(any(), any());

        assertThatNoException().isThrownBy(() ->
                sut.notifyError(new RuntimeException("test"), new ErrorContext("/api/test", "GET", null, null))
        );
    }
}
