package com.fitpet.server.termsmaster.presentation.dto;

import com.fitpet.server.termsmaster.application.dto.TermsAgreementCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;

@Schema(description = "약관 동의 요청")
public record TermsAgreementRequest(
        @Schema(description = "약관 ID (GET /terms 응답의 termsId)", example = "4")
        @NotNull(message = "약관 ID는 필수입니다.")
        Long termsId,

        @Schema(description = "동의 여부 (필수 약관은 반드시 true)", example = "true")
        boolean isAgreed
) {

    public TermsAgreementCommand toCommand() {
        return TermsAgreementCommand.builder()
                .termsId(this.termsId)
                .isAgreed(this.isAgreed)
                .build();
    }

    public static List<TermsAgreementCommand> toCommands(List<TermsAgreementRequest> requests) {
        if (requests == null) return Collections.emptyList();
        return requests.stream().map(TermsAgreementRequest::toCommand).toList();
    }
}