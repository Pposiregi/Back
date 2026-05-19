package com.fitpet.server.shared.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fitpet.server.shared.config.GeminiProperties;
import com.fitpet.server.shared.notification.dto.gemini.GeminiResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestBodySpec;
import org.springframework.web.client.RestClient.RequestBodyUriSpec;
import org.springframework.web.client.RestClient.ResponseSpec;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class GeminiErrorAnalyzerTest {

    @Mock GeminiProperties properties;
    @Mock RestClient geminiRestClient;
    @Mock(answer = Answers.RETURNS_SELF) RequestBodyUriSpec postSpec;
    @Mock(answer = Answers.RETURNS_SELF) RequestBodySpec bodySpec;
    @Mock ResponseSpec responseSpec;

    @InjectMocks GeminiErrorAnalyzer sut;

    private final ErrorContext context = new ErrorContext("/api/test", "GET", null);
    private final RuntimeException throwable = new RuntimeException("test error");

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(sut, "activeProfile", "test");
    }

    @Test
    @DisplayName("analyze - disabled이면 null을 반환하고 API를 호출하지 않는다")
    void analyze_disabled이면_null_반환() {
        when(properties.isEnabled()).thenReturn(false);

        String result = sut.analyze(throwable, context);

        assertThat(result).isNull();
        verify(geminiRestClient, never()).post();
    }

    @Test
    @DisplayName("analyze - apiKey가 비어 있으면 null을 반환한다")
    void analyze_apiKey_없으면_null_반환() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getApiKey()).thenReturn("");

        String result = sut.analyze(throwable, context);

        assertThat(result).isNull();
        verify(geminiRestClient, never()).post();
    }

    @Test
    @DisplayName("analyze - Gemini API를 호출하고 분석 결과를 반환한다")
    void analyze_API_호출_및_결과_반환() {
        GeminiResponse response = new GeminiResponse(List.of(
                new GeminiResponse.Candidate(
                        new GeminiResponse.Content(List.of(
                                new GeminiResponse.Part("AI 분석 결과입니다."))))));

        when(properties.isEnabled()).thenReturn(true);
        when(properties.getApiKey()).thenReturn("test-api-key");
        when(properties.getModel()).thenReturn("gemini-2.5-flash");
        when(geminiRestClient.post()).thenReturn(postSpec);
        when(postSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(GeminiResponse.class)).thenReturn(response);

        String result = sut.analyze(throwable, context);

        assertThat(result).isEqualTo("AI 분석 결과입니다.");
    }

    @Test
    @DisplayName("analyze - Gemini API 호출 실패 시 null을 반환하고 예외를 삼킨다")
    void analyze_API_실패_시_null_반환() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getApiKey()).thenReturn("test-api-key");
        when(geminiRestClient.post()).thenThrow(new RuntimeException("Gemini API timeout"));

        String result = sut.analyze(throwable, context);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("analyze - 응답 candidates가 비어 있으면 null을 반환한다")
    void analyze_빈_candidates_null_반환() {
        GeminiResponse empty = new GeminiResponse(List.of());

        when(properties.isEnabled()).thenReturn(true);
        when(properties.getApiKey()).thenReturn("test-api-key");
        when(properties.getModel()).thenReturn("gemini-2.5-flash");
        when(geminiRestClient.post()).thenReturn(postSpec);
        when(postSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(GeminiResponse.class)).thenReturn(empty);

        String result = sut.analyze(throwable, context);

        assertThat(result).isNull();
    }
}
