package com.fitpet.server.termsmaster.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.springframework.data.annotation.LastModifiedDate;

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
import java.util.Optional;
import java.util.List;
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

    private TermsAgreement agreementOf(User user, Terms terms, boolean isAgreed) {
        return TermsAgreement.builder()
                .user(user)
                .terms(terms)
                .isAgreed(isAgreed)
                .build();
    }

    @Test
    void saveTermsAgreements_Long_userId_진입점에서_존재하지_않는_userId면_BusinessException을_던진다() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.saveTermsAgreements(999L, List.of()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    void saveTermsAgreements_모든_약관에_동의하면_저장된다() {
        User user = testUser();
        Terms service   = termsOf(1L, TermsType.SERVICE_USE,    "2.0");
        Terms privacy   = termsOf(2L, TermsType.PRIVACY_POLICY, "2.0");
        Terms marketing = termsOf(3L, TermsType.MARKETING,      "2.0");

        when(termsRepository.findAllActiveTerms(any())).thenReturn(List.of(service, privacy, marketing));
        when(termsAgreementRepository.findAllByUserId(USER_ID)).thenReturn(List.of());

        sut.saveTermsAgreements(user, List.of(
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
        User user = testUser();
        Terms service   = termsOf(1L, TermsType.SERVICE_USE,    "2.0");
        Terms privacy   = termsOf(2L, TermsType.PRIVACY_POLICY, "2.0");
        Terms marketing = termsOf(3L, TermsType.MARKETING,      "2.0");

        when(termsRepository.findAllActiveTerms(any())).thenReturn(List.of(service, privacy, marketing));
        when(termsAgreementRepository.findAllByUserId(USER_ID)).thenReturn(List.of());

        sut.saveTermsAgreements(user, List.of(
                new TermsAgreementCommand(1L, true),
                new TermsAgreementCommand(2L, true),
                new TermsAgreementCommand(3L, false)  // MARKETING = 선택
        ));

        verify(termsAgreementRepository).saveAll(any());
    }

    @Test
    void saveTermsAgreements_최신_버전이_아닌_termsId_요청시_BusinessException을_던진다() {
        Terms latest = termsOf(10L, TermsType.SERVICE_USE, "2.0");

        when(termsRepository.findAllActiveTerms(any())).thenReturn(List.of(latest));

        assertThatThrownBy(() -> sut.saveTermsAgreements(testUser(), List.of(
                new TermsAgreementCommand(99L, true)  // 구버전 id
        )))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.TERMS_NOT_FOUND.getMessage());
    }

    @Test
    void saveTermsAgreements_필수_약관_미동의시_BusinessException을_던진다() {
        Terms service = termsOf(1L, TermsType.SERVICE_USE, "2.0");  // required=true

        when(termsRepository.findAllActiveTerms(any())).thenReturn(List.of(service));

        assertThatThrownBy(() -> sut.saveTermsAgreements(testUser(), List.of(
                new TermsAgreementCommand(1L, false)
        )))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.REQUIRED_TERMS_NOT_AGREED.getMessage());
    }

    @Test
    void saveTermsAgreements_이미_동의한_약관에_재요청시_updateAgreed가_호출된다() {
        User user = testUser();
        Terms marketing = termsOf(3L, TermsType.MARKETING, "2.0");
        TermsAgreement existingAgreement = agreementOf(user, marketing, true);

        when(termsRepository.findAllActiveTerms(any())).thenReturn(List.of(marketing));
        when(termsAgreementRepository.findAllByUserId(USER_ID)).thenReturn(List.of(existingAgreement));

        sut.saveTermsAgreements(user, List.of(
                new TermsAgreementCommand(3L, false)
        ));

        ArgumentCaptor<List<TermsAgreement>> captor = ArgumentCaptor.forClass(List.class);
        verify(termsAgreementRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0)).isSameAs(existingAgreement);
        assertThat(captor.getValue().get(0).isAgreed()).isFalse();
    }

    @Test
    void saveTermsAgreements_이미_동의한_약관에_재동의시_isAgreed가_true로_유지된다() {
        User user = testUser();
        Terms marketing = termsOf(3L, TermsType.MARKETING, "2.0");
        TermsAgreement existingAgreement = agreementOf(user, marketing, false);

        when(termsRepository.findAllActiveTerms(any())).thenReturn(List.of(marketing));
        when(termsAgreementRepository.findAllByUserId(USER_ID)).thenReturn(List.of(existingAgreement));

        sut.saveTermsAgreements(user, List.of(
                new TermsAgreementCommand(3L, true)
        ));

        ArgumentCaptor<List<TermsAgreement>> captor = ArgumentCaptor.forClass(List.class);
        verify(termsAgreementRepository).saveAll(captor.capture());
        TermsAgreement saved = captor.getValue().get(0);
        assertThat(saved).isSameAs(existingAgreement);
        assertThat(saved.isAgreed()).isTrue();
    }

    @Test
    void saveTermsAgreements_기존_레코드_없으면_새로운_TermsAgreement를_생성한다() {
        User user = testUser();
        Terms marketing = termsOf(3L, TermsType.MARKETING, "2.0");

        when(termsRepository.findAllActiveTerms(any())).thenReturn(List.of(marketing));
        when(termsAgreementRepository.findAllByUserId(USER_ID)).thenReturn(List.of());

        sut.saveTermsAgreements(user, List.of(
                new TermsAgreementCommand(3L, true)
        ));

        ArgumentCaptor<List<TermsAgreement>> captor = ArgumentCaptor.forClass(List.class);
        verify(termsAgreementRepository).saveAll(captor.capture());
        List<TermsAgreement> saved = captor.getValue();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).isAgreed()).isTrue();
        assertThat(saved.get(0).getTerms()).isEqualTo(marketing);
        assertThat(saved.get(0).getUser()).isEqualTo(user);
    }

    @Test
    void saveTermsAgreements_필수_약관이_commands에_누락되면_BusinessException을_던진다() {
        Terms service  = termsOf(1L, TermsType.SERVICE_USE,    "2.0");
        Terms privacy  = termsOf(2L, TermsType.PRIVACY_POLICY, "2.0");

        when(termsRepository.findAllActiveTerms(any())).thenReturn(List.of(service, privacy));

        assertThatThrownBy(() -> sut.saveTermsAgreements(testUser(), List.of(
                new TermsAgreementCommand(1L, true)
        )))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.REQUIRED_TERMS_NOT_AGREED.getMessage());
    }

    @Test
    void saveTermsAgreements_선택_약관만_누락되면_정상_저장된다() {
        User user = testUser();
        Terms service   = termsOf(1L, TermsType.SERVICE_USE,    "2.0");
        Terms privacy   = termsOf(2L, TermsType.PRIVACY_POLICY, "2.0");
        Terms marketing = termsOf(3L, TermsType.MARKETING,      "2.0");

        when(termsRepository.findAllActiveTerms(any())).thenReturn(List.of(service, privacy, marketing));
        when(termsAgreementRepository.findAllByUserId(USER_ID)).thenReturn(List.of());

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> sut.saveTermsAgreements(user, List.of(
                new TermsAgreementCommand(1L, true),
                new TermsAgreementCommand(2L, true)
        )));
    }

    @Test
    void saveTermsAgreements_동일한_termsId가_중복으로_포함되면_BusinessException을_던진다() {
        when(termsRepository.findAllActiveTerms(any())).thenReturn(List.of(
                termsOf(1L, TermsType.SERVICE_USE, "2.0")
        ));

        assertThatThrownBy(() -> sut.saveTermsAgreements(testUser(), List.of(
                new TermsAgreementCommand(1L, true),
                new TermsAgreementCommand(1L, false)
        )))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.INVALID_TERMS_REQUEST.getMessage());
    }

    @Test
    void saveTermsAgreements_신규_TermsType_필수_약관_미동의시_BusinessException을_던진다() {
        Terms privacyCollection = termsOf(4L, TermsType.PRIVACY_COLLECTION, "2.0");

        when(termsRepository.findAllActiveTerms(any())).thenReturn(List.of(privacyCollection));

        assertThatThrownBy(() -> sut.saveTermsAgreements(testUser(), List.of(
                new TermsAgreementCommand(4L, false)
        )))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.REQUIRED_TERMS_NOT_AGREED.getMessage());
    }

    @Test
    void termsAgreement_엔티티에_updatedAt_필드와_LastModifiedDate_애노테이션이_존재한다() throws Exception {
        var field = TermsAgreement.class.getDeclaredField("updatedAt");
        assertThat(field.isAnnotationPresent(LastModifiedDate.class)).isTrue();
    }
}
