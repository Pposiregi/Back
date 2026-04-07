package com.fitpet.server.termsmaster.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fitpet.server.shared.exception.BusinessException;
import com.fitpet.server.shared.exception.ErrorCode;
import com.fitpet.server.termsmaster.application.dto.TermsAgreementCommand;
import com.fitpet.server.termsmaster.domain.entity.Terms;
import com.fitpet.server.termsmaster.domain.entity.TermsAgreement;
import com.fitpet.server.termsmaster.domain.entity.TermsType;
import com.fitpet.server.termsmaster.domain.repository.TermsAgreementRepository;
import com.fitpet.server.termsmaster.domain.repository.TermsRepository;
import com.fitpet.server.user.domain.entity.User;
import com.fitpet.server.user.domain.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TermsAgreementServiceTest {

    @Mock TermsRepository termsRepository;
    @Mock TermsAgreementRepository termsAgreementRepository;
    @Mock UserRepository userRepository;

    @InjectMocks TermsAgreementService sut;

    private static final Long USER_ID = 1L;

    private User testUser() {
        return User.builder().id(USER_ID).email("test@test.com").build();
    }

    private Terms termsOf(Long id, TermsType type, String version) {
        Terms t = Terms.builder()
                .code(type).content("내용").version(version)
                .effectiveDate(LocalDate.of(2025, 1, 1)).build();
        try {
            var field = Terms.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(t, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return t;
    }


    @Test
    void saveTermsAgreements_모든_약관에_동의하면_저장된다() {
        Terms service   = termsOf(1L, TermsType.SERVICE_USE,    "2.0");
        Terms privacy   = termsOf(2L, TermsType.PRIVACY_POLICY, "2.0");
        Terms marketing = termsOf(3L, TermsType.MARKETING,      "2.0");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser()));
        when(termsRepository.findAllActiveTerms(any())).thenReturn(List.of(service, privacy, marketing));

        sut.saveTermsAgreements(USER_ID, List.of(
                new TermsAgreementCommand(1L, true),
                new TermsAgreementCommand(2L, true),
                new TermsAgreementCommand(3L, true)
        ));

        ArgumentCaptor<List<TermsAgreement>> captor = ArgumentCaptor.forClass(List.class);
        verify(termsAgreementRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(3);
    }

    @Test
    void saveTermsAgreements_선택_약관_미동의해도_저장된다() {
        Terms service   = termsOf(1L, TermsType.SERVICE_USE,    "2.0");
        Terms privacy   = termsOf(2L, TermsType.PRIVACY_POLICY, "2.0");
        Terms marketing = termsOf(3L, TermsType.MARKETING,      "2.0");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser()));
        when(termsRepository.findAllActiveTerms(any())).thenReturn(List.of(service, privacy, marketing));

        sut.saveTermsAgreements(USER_ID, List.of(
                new TermsAgreementCommand(1L, true),
                new TermsAgreementCommand(2L, true),
                new TermsAgreementCommand(3L, false)  // MARKETING = 선택
        ));

        verify(termsAgreementRepository).saveAll(any());
    }

    @Test
    void saveTermsAgreements_존재하지_않는_userId로_요청시_BusinessException을_던진다() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.saveTermsAgreements(999L, List.of()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    void saveTermsAgreements_최신_버전이_아닌_termsId_요청시_BusinessException을_던진다() {
        Terms latest = termsOf(10L, TermsType.SERVICE_USE, "2.0");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser()));
        when(termsRepository.findAllActiveTerms(any())).thenReturn(List.of(latest));

        assertThatThrownBy(() -> sut.saveTermsAgreements(USER_ID, List.of(
                new TermsAgreementCommand(99L, true)  // 구버전 id
        )))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.TERMS_NOT_FOUND.getMessage());
    }

    @Test
    void saveTermsAgreements_필수_약관_미동의시_BusinessException을_던진다() {
        Terms service = termsOf(1L, TermsType.SERVICE_USE, "2.0");  // required=true

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser()));
        when(termsRepository.findAllActiveTerms(any())).thenReturn(List.of(service));

        assertThatThrownBy(() -> sut.saveTermsAgreements(USER_ID, List.of(
                new TermsAgreementCommand(1L, false)
        )))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.REQUIRED_TERMS_NOT_AGREED.getMessage());
    }
}
