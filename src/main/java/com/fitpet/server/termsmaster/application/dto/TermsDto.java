package com.fitpet.server.termsmaster.application.dto;

import com.fitpet.server.termsmaster.domain.entity.Terms;
import com.fitpet.server.termsmaster.domain.entity.TermsType;
import java.time.LocalDate;
import lombok.Builder;

@Builder
public record TermsDto(
        Long termsId,
        TermsType termsCode,
        String title,
        String content,
        boolean isRequired,
        String version,
        LocalDate effectiveDate
) {
    public static TermsDto from(Terms terms) {
        return TermsDto.builder()
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