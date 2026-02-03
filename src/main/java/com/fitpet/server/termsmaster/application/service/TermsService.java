package com.fitpet.server.termsmaster.application.service;

import com.fitpet.server.termsmaster.application.dto.TermsDto;
import com.fitpet.server.termsmaster.domain.entity.Terms;
import com.fitpet.server.termsmaster.domain.entity.TermsType;
import com.fitpet.server.termsmaster.domain.repository.TermsRepository;
import java.time.LocalDate;
import java.util.ArrayList;
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

    @Cacheable(cacheNames = "terms", key = "'active'")
    public List<TermsDto> getActiveTerms() {
        log.info("[TermsService] DB에서 약관을 조회합니다.");

        List<Terms> activeTerms = termsRepository.findAllActiveTerms(LocalDate.now());

        return new ArrayList<>(activeTerms.stream()
                .map(TermsDto::from)
                .toList());
    }

    @Transactional
    @CacheEvict(value = "terms", allEntries = true)
    public void createTerms(TermsType code, String content, String version) {

        Terms newTerms = Terms.builder()
                .code(code)
                .content(content)
                .version(version)
                .effectiveDate(LocalDate.now())
                .build();

        termsRepository.save(newTerms);
    }
}