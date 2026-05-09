package com.fitpet.server.termsmaster.presentation.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TermsAgreementRequestTest {

    @Test
    void toCommands_null_입력시_NPE_대신_빈_리스트를_반환한다() {
        assertThat(TermsAgreementRequest.toCommands(null)).isEmpty();
    }

    @Test
    void toCommands_정상_입력시_commands로_변환된다() {
        var requests = java.util.List.of(
                new TermsAgreementRequest(1L, true),
                new TermsAgreementRequest(2L, false)
        );
        var commands = TermsAgreementRequest.toCommands(requests);
        assertThat(commands).hasSize(2);
        assertThat(commands.get(0).termsId()).isEqualTo(1L);
        assertThat(commands.get(1).isAgreed()).isFalse();
    }
}
