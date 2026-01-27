package com.fitpet.server.termsmaster.presentation.dto;

import com.fitpet.server.termsmaster.domain.entity.TermsType;

public record TermsCreateRequest(
        TermsType code,
        String content,
        String version
) {
}
