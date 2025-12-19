package com.fitpet.server.termsmaster.presentation.dto;

import com.fitpet.server.termsmaster.application.dto.TermsDto;
import com.fitpet.server.termsmaster.domain.entity.TermsType;
import java.time.LocalDate;
import lombok.Builder;

@Builder
public record TermsResponse(
        Long termsId,
        TermsType termsCode,
        String title,
        String content,
        boolean isRequired,
        String version,
        LocalDate effectiveDate
) {
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