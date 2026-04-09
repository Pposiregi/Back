package com.fitpet.server.termsmaster.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.springframework.data.annotation.CreatedDate;

import com.fitpet.server.shared.exception.BusinessException;
import com.fitpet.server.shared.exception.ErrorCode;
import com.fitpet.server.termsmaster.application.dto.TermsDto;
import com.fitpet.server.termsmaster.domain.entity.Terms;
import com.fitpet.server.termsmaster.domain.entity.TermsType;
import com.fitpet.server.termsmaster.domain.repository.TermsRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TermsServiceTest {

    @Mock
    TermsRepository termsRepository;

    @InjectMocks
    TermsService sut;

    @Test
    void getTerms_version이_null이면_최신_약관을_반환한다() {
        Terms t = Terms.builder()
                .code(TermsType.SERVICE_USE).content("내용").version("2.0")
                .effectiveDate(LocalDate.of(2025, 1, 1)).build();
        when(termsRepository.findAllActiveTerms(any())).thenReturn(List.of(t));

        List<TermsDto> result = sut.getTerms(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).version()).isEqualTo("2.0");
    }

    @Test
    void getTerms_version이_있으면_해당_버전_약관을_반환한다() {
        Terms t = Terms.builder()
                .code(TermsType.SERVICE_USE).content("내용").version("1.0")
                .effectiveDate(LocalDate.of(2024, 1, 1)).build();
        when(termsRepository.findAllByVersion("1.0")).thenReturn(List.of(t));

        List<TermsDto> result = sut.getTerms("1.0");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).version()).isEqualTo("1.0");
    }

    @Test
    void getActiveTerms_최신_버전_약관_리스트를_반환한다() {
        Terms t1 = Terms.builder()
                .code(TermsType.SERVICE_USE).content("내용").version("2.0")
                .effectiveDate(LocalDate.of(2025, 1, 1)).build();
        Terms t2 = Terms.builder()
                .code(TermsType.PRIVACY_POLICY).content("내용").version("2.0")
                .effectiveDate(LocalDate.of(2025, 1, 1)).build();
        when(termsRepository.findAllActiveTerms(any())).thenReturn(List.of(t1, t2));

        List<TermsDto> result = sut.getActiveTerms();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(TermsDto::version).containsOnly("2.0");
    }

    @Test
    void getActiveTerms_활성_약관이_없으면_빈_리스트를_반환한다() {
        when(termsRepository.findAllActiveTerms(any())).thenReturn(List.of());

        List<TermsDto> result = sut.getActiveTerms();

        assertThat(result).isEmpty();
    }

    @Test
    void getTermsByVersion_존재하는_버전_조회시_해당_버전_리스트를_반환한다() {
        Terms t = Terms.builder()
                .code(TermsType.MARKETING).content("내용").version("1.0")
                .effectiveDate(LocalDate.of(2024, 1, 1)).build();
        when(termsRepository.findAllByVersion("1.0")).thenReturn(List.of(t));

        List<TermsDto> result = sut.getTermsByVersion("1.0");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).version()).isEqualTo("1.0");
        assertThat(result.get(0).termsCode()).isEqualTo(TermsType.MARKETING);
    }

    @Test
    void getTermsByVersion_존재하지_않는_버전_조회시_BusinessException을_던진다() {
        when(termsRepository.findAllByVersion("9.9")).thenReturn(List.of());

        assertThatThrownBy(() -> sut.getTermsByVersion("9.9"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.TERMS_NOT_FOUND.getMessage());
    }

    @Test
    void getTerms_메서드에_Cacheable_애노테이션이_존재한다() throws Exception {
        var method = TermsService.class.getMethod("getTerms", String.class);
        assertThat(method.isAnnotationPresent(org.springframework.cache.annotation.Cacheable.class)).isTrue();
    }

    @Test
    void getActiveTerms_메서드에_Cacheable_애노테이션이_없다() throws Exception {
        var method = TermsService.class.getMethod("getActiveTerms");
        assertThat(method.isAnnotationPresent(org.springframework.cache.annotation.Cacheable.class)).isFalse();
    }

    @Test
    void terms_엔티티에_createdAt_필드와_CreatedDate_애노테이션이_존재한다() throws Exception {
        var field = Terms.class.getDeclaredField("createdAt");
        assertThat(field.isAnnotationPresent(CreatedDate.class)).isTrue();
    }

    @Test
    void createTerms_호출시_Terms_엔티티가_저장된다() {
        ArgumentCaptor<Terms> captor = ArgumentCaptor.forClass(Terms.class);

        sut.createTerms(TermsType.SERVICE_USE, "서비스 이용약관 v3.0", "3.0");

        verify(termsRepository).save(captor.capture());
        Terms saved = captor.getValue();
        assertThat(saved.getCode()).isEqualTo(TermsType.SERVICE_USE);
        assertThat(saved.getVersion()).isEqualTo("3.0");
        assertThat(saved.getContent()).isEqualTo("서비스 이용약관 v3.0");
        assertThat(saved.getEffectiveDate()).isEqualTo(LocalDate.now());
    }
}
