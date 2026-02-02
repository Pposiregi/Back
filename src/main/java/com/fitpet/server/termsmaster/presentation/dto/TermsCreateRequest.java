package com.fitpet.server.termsmaster.presentation.dto;

import com.fitpet.server.termsmaster.domain.entity.TermsType;
import jakarta.validation.constraints.NotBlank;
import org.codehaus.commons.nullanalysis.NotNull;

public record TermsCreateRequest(
        @NotNull TermsType code,
        @NotBlank String content,
        @NotBlank String version
) {
}
