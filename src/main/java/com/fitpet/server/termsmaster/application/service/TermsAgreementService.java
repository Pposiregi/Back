package com.fitpet.server.termsmaster.application.service;

import com.fitpet.server.shared.exception.BusinessException;
import com.fitpet.server.shared.exception.ErrorCode;
import com.fitpet.server.terms.domain.entity.TermsAgreement;
import com.fitpet.server.termsmaster.application.dto.TermsAgreementCommand;
import com.fitpet.server.termsmaster.domain.entity.Terms;
import com.fitpet.server.termsmaster.domain.repository.TermsAgreementRepository;
import com.fitpet.server.termsmaster.domain.repository.TermsRepository;
import com.fitpet.server.user.domain.entity.User;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TermsAgreementService {

    private final TermsRepository termsRepository;
    private final TermsAgreementRepository termsAgreementRepository;

    public void saveTermsAgreements(User user, List<TermsAgreementCommand> requests) {
        List<Terms> activeTerms = termsRepository.findAllActiveTerms(LocalDate.now());

        List<TermsAgreement> agreementsToSave = new ArrayList<>();

        for (TermsAgreementCommand request : requests) {
            Terms terms = activeTerms.stream()
                    .filter(t -> t.getId().equals(request.termsId()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(ErrorCode.TERMS_NOT_FOUND));

            // 필수 약관 검증
            if (terms.getCode().isRequired() && !request.isAgreed()) {
                throw new BusinessException(ErrorCode.REQUIRED_TERMS_NOT_AGREED);
            }

            agreementsToSave.add(TermsAgreement.builder()
                    .user(user)
                    .terms(terms)
                    .isAgreed(request.isAgreed())
                    .build());
        }

        termsAgreementRepository.saveAll(agreementsToSave);
    }
}