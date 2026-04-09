package com.fitpet.server.termsmaster.presentation.dto;

import com.fitpet.server.termsmaster.application.dto.TermsDto;
import com.fitpet.server.termsmaster.domain.entity.TermsType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "약관 응답")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TermsResponse {

    @Schema(description = "약관 ID", example = "4")
    private Long termsId;

    @Schema(description = "약관 타입", example = "SERVICE_USE")
    private TermsType termsCode;

    @Schema(description = "약관 제목", example = "서비스 이용약관")
    private String title;

    @Schema(description = "약관 본문 내용")
    private String content;

    @Schema(description = "필수 동의 여부", example = "true")
    private boolean isRequired;

    @Schema(description = "약관 버전", example = "2.0")
    private String version;

    @Schema(description = "약관 시행일", example = "2025-01-01")
    private LocalDate effectiveDate;

    public static TermsResponse from(TermsDto dto) {
        return TermsResponse.builder()
                .termsId(dto.termsId())
                .termsCode(dto.termsCode())
                .title(dto.title())
                .content(dto.content())
                .isRequired(dto.isRequired())
                .version(dto.version())
                .effectiveDate(dto.effectiveDate())
                .build();
    }
}