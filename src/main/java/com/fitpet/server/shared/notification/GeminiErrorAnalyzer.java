package com.fitpet.server.shared.notification;

import com.fitpet.server.shared.config.GeminiProperties;
import com.fitpet.server.shared.notification.dto.gemini.GeminiRequest;
import com.fitpet.server.shared.notification.dto.gemini.GeminiResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiErrorAnalyzer {

    private static final int MAX_ANALYSIS_LENGTH = 1000;
    private static final String API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={key}";

    private final GeminiProperties properties;
    private final RestClient geminiRestClient;

    @Value("${spring.profiles.active:local}")
    private String activeProfile;

    public String analyze(Throwable throwable, ErrorContext context) {
        if (!properties.isEnabled() || !StringUtils.hasText(properties.getApiKey())) {
            return null;
        }

        try {
            return callGemini(throwable, context);
        } catch (Exception e) {
            log.warn("Gemini analysis failed, proceeding without AI analysis", e);
            return null;
        }
    }

    private String callGemini(Throwable throwable, ErrorContext context) {
        GeminiRequest request = GeminiRequest.of(buildPrompt(throwable, context));

        GeminiResponse response = geminiRestClient.post()
                .uri(API_URL, properties.getModel(), properties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(GeminiResponse.class);

        if (response == null) return null;
        List<GeminiResponse.Candidate> candidates = response.candidates();
        if (candidates == null || candidates.isEmpty()) return null;

        String text = candidates.get(0).content().parts().get(0).text();
        return text.length() > MAX_ANALYSIS_LENGTH ? text.substring(0, MAX_ANALYSIS_LENGTH) + "..." : text;
    }

    private static final int STACK_TOP_LINES = 5;

    private String buildPrompt(Throwable throwable, ErrorContext context) {
        String topFrames = getStackTrace(throwable).lines()
                .limit(STACK_TOP_LINES)
                .reduce("", (a, b) -> a + b + "\n")
                .strip();

        return String.format("""
                당신은 Spring Boot 3 + JPA + Redis 기반 헥사고날/DDD 백엔드를 운영하는 시니어 엔지니어입니다.
                아래 예외를 분석하세요.

                [환경] %s
                [요청] %s %s
                [예외] %s: %s
                [스택 트레이스 상위]
                %s

                아래 형식을 정확히 지켜 한국어로만, 매우 간결하게 답하세요.
                전체 450자 이내. 각 항목은 한 문장. 불필요한 수식어·코드블록·재진술 금지. 추측은 "추정:" 표기.

                **요약**: (한 문장으로 무슨 에러인지)
                **원인**: (핵심 근본 원인 한 문장)
                **해결**: (가장 먼저 할 조치 1~2개, 짧게)
                **긴급도**: (높음/보통/낮음 + 한 줄 사유)
                """,
                activeProfile,
                context.method(), context.uri(),
                throwable.getClass().getSimpleName(), throwable.getMessage(),
                topFrames
        );
    }

    private String getStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
