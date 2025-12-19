package com.fitpet.server.termsmaster.application.dto;

import lombok.Builder;

@Builder
public record TermsAgreementCommand(
        Long termsId,
        boolean isAgreed
) {
}