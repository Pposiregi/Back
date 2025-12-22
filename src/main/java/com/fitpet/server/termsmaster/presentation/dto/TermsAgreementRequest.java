package com.fitpet.server.termsmaster.presentation.dto;

import com.fitpet.server.termsmaster.application.dto.TermsAgreementCommand;
import jakarta.validation.constraints.NotNull;

public record TermsAgreementRequest(
        @NotNull(message = "약관 ID는 필수입니다.")
        Long termsId,

        boolean isAgreed
) {

    public TermsAgreementCommand toCommand() {
        return TermsAgreementCommand.builder()
                .termsId(this.termsId)
                .isAgreed(this.isAgreed)
                .build();
    }
}