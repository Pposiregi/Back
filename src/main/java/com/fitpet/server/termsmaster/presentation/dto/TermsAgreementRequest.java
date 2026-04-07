package com.fitpet.server.termsmaster.presentation.dto;

import com.fitpet.server.termsmaster.application.dto.TermsAgreementCommand;
import jakarta.validation.constraints.NotNull;
import java.util.List;

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

    public static List<TermsAgreementCommand> toCommands(List<TermsAgreementRequest> requests) {
        return requests.stream().map(TermsAgreementRequest::toCommand).toList();
    }
}