package com.fitpet.server.shared.notification;

import com.fitpet.server.shared.config.DiscordWebhookProperties;
import com.fitpet.server.shared.notification.dto.DiscordEmbed;
import com.fitpet.server.shared.notification.dto.DiscordEmbedField;
import com.fitpet.server.shared.notification.dto.DiscordWebhookPayload;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscordNotificationService implements NotificationService {

    private static final int ERROR_COLOR = 0xED4245;
    private static final int SIGNUP_COLOR = 0x57F287;
    private static final int MAX_STACK_LENGTH = 1900;
    private static final int STACK_TOP_LINES = 5;

    private final DiscordWebhookClient webhookClient;
    private final DiscordWebhookProperties properties;
    private final GeminiErrorAnalyzer geminiErrorAnalyzer;

    @Override
    @Async("notificationExecutor")
    public void notifyError(Throwable throwable, ErrorContext context) {
        if (!properties.isEnabled()) return;
        try {
            String aiAnalysis = geminiErrorAnalyzer.analyze(throwable, context);
            webhookClient.send(properties.getErrorWebhookUrl(), buildErrorPayload(throwable, context, aiAnalysis));
        } catch (Exception e) {
            log.error("Discord error notification failed", e);
        }
    }

    @Override
    public void notifySignup(Long userId, long totalCount) {
        if (!properties.isEnabled()) return;
        try {
            webhookClient.send(properties.getSignupWebhookUrl(), buildSignupPayload(userId, totalCount));
        } catch (Exception e) {
            log.error("Discord signup notification failed", e);
        }
    }

    private DiscordWebhookPayload buildErrorPayload(Throwable throwable, ErrorContext context, String aiAnalysis) {
        List<DiscordEmbedField> fields = new ArrayList<>();
        fields.add(new DiscordEmbedField("URI", context.uri(), true));
        fields.add(new DiscordEmbedField("Method", context.method(), true));
        fields.add(new DiscordEmbedField("User", context.userId() != null ? "#" + context.userId() : "비인증", true));
        if (StringUtils.hasText(context.queryString())) {
            fields.add(new DiscordEmbedField("Query", context.queryString(), false));
        }
        if (StringUtils.hasText(aiAnalysis)) {
            fields.add(new DiscordEmbedField("🤖 AI 분석", aiAnalysis, false));
        }
        DiscordEmbed embed = new DiscordEmbed(
                "🚨 " + throwable.getClass().getSimpleName(),
                "```" + truncateStackTrace(throwable) + "```",
                ERROR_COLOR,
                fields
        );
        return DiscordWebhookPayload.of(embed);
    }

    private DiscordWebhookPayload buildSignupPayload(Long userId, long totalCount) {
        DiscordEmbed embed = new DiscordEmbed(
                "🎉 신규 회원 가입",
                totalCount + "번째 회원 #" + userId + " 회원가입 완료!",
                SIGNUP_COLOR,
                List.of()
        );
        return DiscordWebhookPayload.of(embed);
    }

    private String truncateStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        String stack = sw.toString().lines()
                .limit(STACK_TOP_LINES)
                .reduce("", (acc, line) -> acc + line + "\n")
                .strip();
        return stack.length() > MAX_STACK_LENGTH ? stack.substring(0, MAX_STACK_LENGTH) + "..." : stack;
    }
}
