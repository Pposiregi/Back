package com.fitpet.server.termsmaster.presentation.dto;

import com.fitpet.server.termsmaster.domain.entity.Terms;
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
    public static TermsResponse from(Terms terms) {
        return TermsResponse.builder()
                .termsId(terms.getId())
                .termsCode(terms.getCode())
                .title(terms.getCode().getDescription())
                .isRequired(terms.getCode().isRequired())
                .content(terms.getContent())
                .version(terms.getVersion())
                .effectiveDate(terms.getEffectiveDate())
                .build();
    }
}