package com.fitpet.server.termsmaster.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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

    private TermsAgreement agreementOf(User user, Terms terms, boolean isAgreed) {
        return TermsAgreement.builder()
                .user(user)
                .terms(terms)
                .isAgreed(isAgreed)
                .build();
    }

    @Test
    void saveTermsAgreements_모든_약관에_동의하면_저장된다() {
        Terms service   = termsOf(1L, TermsType.SERVICE_USE,    "2.0");
        Terms privacy   = termsOf(2L, TermsType.PRIVACY_POLICY, "2.0");
        Terms marketing = termsOf(3L, TermsType.MARKETING,      "2.0");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser()));
        when(termsRepository.findAllActiveTerms(any())).thenReturn(List.of(service, privacy, marketing));
        // INSERT 경로: 기존 레코드 없음
        when(termsAgreementRepository.findByUserIdAndTermsId(eq(USER_ID), anyLong()))
                .thenReturn(Optional.empty());

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
        when(termsAgreementRepository.findByUserIdAndTermsId(eq(USER_ID), anyLong()))
                .thenReturn(Optional.empty());

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
        // validateRequiredAgreement 에서 먼저 예외 발생 → findByUserIdAndTermsId 호출 안 됨

        assertThatThrownBy(() -> sut.saveTermsAgreements(USER_ID, List.of(
                new TermsAgreementCommand(1L, false)
        )))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.REQUIRED_TERMS_NOT_AGREED.getMessage());
    }

    @Test
    void saveTermsAgreements_이미_동의한_약관에_재요청시_updateAgreed가_호출된다() {
        // given: MARKETING(선택)은 required=false → isAgreed=false 로 재요청해도 예외 없음
        User user = testUser();
        Terms marketing = termsOf(3L, TermsType.MARKETING, "2.0");
        TermsAgreement existingAgreement = agreementOf(user, marketing, true);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(termsRepository.findAllActiveTerms(any())).thenReturn(List.of(marketing));
        // UPDATE 경로: 기존 레코드 존재
        when(termsAgreementRepository.findByUserIdAndTermsId(USER_ID, 3L))
                .thenReturn(Optional.of(existingAgreement));

        // when: 동의 → 철회
        sut.saveTermsAgreements(USER_ID, List.of(
                new TermsAgreementCommand(3L, false)
        ));

        // then: saveAll 에 기존 객체(UPDATE)가 담겨야 함 — 새 인스턴스(INSERT)가 아님
        ArgumentCaptor<List<TermsAgreement>> captor = ArgumentCaptor.forClass(List.class);
        verify(termsAgreementRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0)).isSameAs(existingAgreement);
        assertThat(captor.getValue().get(0).isAgreed()).isFalse();
    }

    @Test
    void saveTermsAgreements_이미_동의한_약관에_재동의시_isAgreed가_true로_유지된다() {
        // given
        User user = testUser();
        Terms marketing = termsOf(3L, TermsType.MARKETING, "2.0");
        TermsAgreement existingAgreement = agreementOf(user, marketing, false);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(termsRepository.findAllActiveTerms(any())).thenReturn(List.of(marketing));
        when(termsAgreementRepository.findByUserIdAndTermsId(USER_ID, 3L))
                .thenReturn(Optional.of(existingAgreement));

        // when: 기존 false → true 로 변경
        sut.saveTermsAgreements(USER_ID, List.of(
                new TermsAgreementCommand(3L, true)
        ));

        // then: updateAgreed(true) 가 적용된 기존 객체가 saveAll 에 전달됨
        ArgumentCaptor<List<TermsAgreement>> captor = ArgumentCaptor.forClass(List.class);
        verify(termsAgreementRepository).saveAll(captor.capture());
        TermsAgreement saved = captor.getValue().get(0);
        assertThat(saved).isSameAs(existingAgreement);
        assertThat(saved.isAgreed()).isTrue();
    }

    @Test
    void saveTermsAgreements_기존_레코드_없으면_새로운_TermsAgreement를_생성한다() {
        // given
        User user = testUser();
        Terms marketing = termsOf(3L, TermsType.MARKETING, "2.0");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(termsRepository.findAllActiveTerms(any())).thenReturn(List.of(marketing));
        // INSERT 경로: 기존 레코드 없음
        when(termsAgreementRepository.findByUserIdAndTermsId(USER_ID, 3L))
                .thenReturn(Optional.empty());

        // when
        sut.saveTermsAgreements(USER_ID, List.of(
                new TermsAgreementCommand(3L, true)
        ));

        // then: 새 객체가 saveAll 에 전달됨 (existingAgreement 인스턴스와 다름)
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
        // given: 필수 약관 2개 활성화
        Terms service  = termsOf(1L, TermsType.SERVICE_USE,    "2.0");  // required=true
        Terms privacy  = termsOf(2L, TermsType.PRIVACY_POLICY, "2.0");  // required=true

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser()));
        when(termsRepository.findAllActiveTerms(any())).thenReturn(List.of(service, privacy));

        // when: SERVICE_USE만 보내고 PRIVACY_POLICY 누락
        assertThatThrownBy(() -> sut.saveTermsAgreements(USER_ID, List.of(
                new TermsAgreementCommand(1L, true)
        )))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.REQUIRED_TERMS_NOT_AGREED.getMessage());
    }

    @Test
    void saveTermsAgreements_선택_약관만_누락되면_정상_저장된다() {
        // given: 필수 2개 + 선택 1개 활성화
        Terms service   = termsOf(1L, TermsType.SERVICE_USE,    "2.0");  // required=true
        Terms privacy   = termsOf(2L, TermsType.PRIVACY_POLICY, "2.0");  // required=true
        Terms marketing = termsOf(3L, TermsType.MARKETING,      "2.0");  // required=false

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser()));
        when(termsRepository.findAllActiveTerms(any())).thenReturn(List.of(service, privacy, marketing));
        when(termsAgreementRepository.findByUserIdAndTermsId(eq(USER_ID), anyLong()))
                .thenReturn(Optional.empty());

        // when: 선택 약관(MARKETING) 누락, 필수만 포함 → 정상 저장
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> sut.saveTermsAgreements(USER_ID, List.of(
                new TermsAgreementCommand(1L, true),
                new TermsAgreementCommand(2L, true)
        )));
    }

    @Test
    void saveTermsAgreements_동일한_termsId가_중복으로_포함되면_BusinessException을_던진다() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser()));
        when(termsRepository.findAllActiveTerms(any())).thenReturn(List.of(
                termsOf(1L, TermsType.SERVICE_USE, "2.0")
        ));

        assertThatThrownBy(() -> sut.saveTermsAgreements(USER_ID, List.of(
                new TermsAgreementCommand(1L, true),
                new TermsAgreementCommand(1L, false)  // 중복
        )))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.INVALID_TERMS_REQUEST.getMessage());
    }

    @Test
    void saveTermsAgreements_신규_TermsType_필수_약관_미동의시_BusinessException을_던진다() {
        Terms privacyCollection = termsOf(4L, TermsType.PRIVACY_COLLECTION, "2.0");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser()));
        when(termsRepository.findAllActiveTerms(any()))
                .thenReturn(List.of(privacyCollection));
        // PRIVACY_COLLECTION 은 required=true → validateRequiredAgreement 에서 바로 예외
        // findByUserIdAndTermsId 는 호출되지 않으므로 stub 불필요

        assertThatThrownBy(() -> sut.saveTermsAgreements(USER_ID, List.of(
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
