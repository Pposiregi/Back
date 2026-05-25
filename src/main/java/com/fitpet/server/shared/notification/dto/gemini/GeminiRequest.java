package com.fitpet.server.shared.notification.dto.gemini;

import java.util.List;

public record GeminiRequest(List<Content> contents, GenerationConfig generationConfig) {

    public record Content(List<Part> parts) {}
    public record Part(String text) {}
    public record GenerationConfig(int maxOutputTokens, ThinkingConfig thinkingConfig) {}
    public record ThinkingConfig(int thinkingBudget) {}

    public static GeminiRequest of(String prompt) {
        return new GeminiRequest(
                List.of(new Content(List.of(new Part(prompt)))),
                new GenerationConfig(1024, new ThinkingConfig(0))
        );
    }
}
