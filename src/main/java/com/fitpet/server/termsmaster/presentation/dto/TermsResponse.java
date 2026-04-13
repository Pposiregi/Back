package com.fitpet.server.termsmaster.presentation.dto;

import com.fitpet.server.termsmaster.application.dto.TermsDto;
import com.fitpet.server.termsmaster.domain.entity.TermsType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "약관 응답")
public record TermsResponse(
        @Schema(description = "약관 ID", example = "4") Long termsId,
        @Schema(description = "약관 타입", example = "SERVICE_USE") TermsType termsCode,
        @Schema(description = "약관 제목", example = "서비스 이용약관") String title,
        @Schema(description = "약관 본문 내용") String content,
        @Schema(description = "필수 동의 여부", example = "true") boolean isRequired,
        @Schema(description = "약관 버전", example = "2.0") String version,
        @Schema(description = "약관 시행일", example = "2025-01-01") LocalDate effectiveDate
) {
    public static TermsResponse from(TermsDto dto) {
        return new TermsResponse(
                dto.termsId(), dto.termsCode(), dto.title(), dto.content(),
                dto.isRequired(), dto.version(), dto.effectiveDate()
        );
    }
}
