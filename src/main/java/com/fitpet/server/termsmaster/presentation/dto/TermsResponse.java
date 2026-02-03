package com.fitpet.server.termsmaster.presentation.dto;

import com.fitpet.server.termsmaster.application.dto.TermsDto;
import com.fitpet.server.termsmaster.domain.entity.TermsType;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TermsResponse {

    private Long termsId;
    private TermsType termsCode;
    private String title;
    private String content;
    private boolean isRequired;
    private String version;
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