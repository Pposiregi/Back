package com.fitpet.server.termsmaster.application.service;

import com.fitpet.server.termsmaster.application.dto.TermsDto;
import com.fitpet.server.termsmaster.domain.entity.Terms;
import com.fitpet.server.termsmaster.domain.repository.TermsRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TermsService {

    private final TermsRepository termsRepository;

    public List<TermsDto> getActiveTerms() {
        List<Terms> activeTerms = termsRepository.findAllActiveTerms(LocalDate.now());

        return activeTerms.stream()
                .map(TermsDto::from)
                .toList();
    }
}