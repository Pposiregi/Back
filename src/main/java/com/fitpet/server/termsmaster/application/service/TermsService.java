package com.fitpet.server.termsmaster.application.service;

import com.fitpet.server.shared.exception.BusinessException;
import com.fitpet.server.shared.exception.ErrorCode;
import com.fitpet.server.termsmaster.application.dto.TermsDto;
import com.fitpet.server.termsmaster.domain.entity.Terms;
import com.fitpet.server.termsmaster.domain.entity.TermsType;
import com.fitpet.server.termsmaster.domain.repository.TermsRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TermsService {

    private final TermsRepository termsRepository;

    public List<TermsDto> getTerms(String version) {
        return (version != null) ? getTermsByVersion(version) : getActiveTerms();
    }

    @Cacheable(cacheNames = "terms", key = "'active'")
    public List<TermsDto> getActiveTerms() {
        log.info("[TermsService] DB에서 약관을 조회합니다.");

        return termsRepository.findAllActiveTerms(LocalDate.now()).stream()
                .map(TermsDto::from)
                .toList();
    }

    public List<TermsDto> getTermsByVersion(String version) {
        List<Terms> terms = termsRepository.findAllByVersion(version);

        if (terms.isEmpty()) {
            throw new BusinessException(ErrorCode.TERMS_NOT_FOUND);
        }

        return terms.stream()
                .map(TermsDto::from)
                .toList();
    }

    @Transactional
    @CacheEvict(value = "terms", allEntries = true)
    public void createTerms(TermsType code, String content, String version) {
        termsRepository.findByCodeAndVersion(code, version)
                .ifPresent(t -> { throw new BusinessException(ErrorCode.DUPLICATE_TERMS); });

        Terms newTerms = Terms.builder()
                .code(code)
                .content(content)
                .version(version)
                .effectiveDate(LocalDate.now())
                .build();

        termsRepository.save(newTerms);
    }
}
