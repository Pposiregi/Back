package com.fitpet.server.termsmaster.presentation.dto;

import com.fitpet.server.termsmaster.domain.entity.TermsType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TermsCreateRequest(
        @NotNull TermsType code,
        @NotBlank String content,
        @NotBlank String version
) {
}
