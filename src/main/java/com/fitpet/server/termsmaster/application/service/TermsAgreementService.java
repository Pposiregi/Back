package com.fitpet.server.termsmaster.application.service;

import com.fitpet.server.shared.exception.BusinessException;
import com.fitpet.server.shared.exception.ErrorCode;
import com.fitpet.server.termsmaster.application.dto.TermsAgreementCommand;
import com.fitpet.server.termsmaster.domain.entity.Terms;
import com.fitpet.server.termsmaster.domain.entity.TermsAgreement;
import com.fitpet.server.termsmaster.domain.repository.TermsAgreementRepository;
import com.fitpet.server.termsmaster.domain.repository.TermsRepository;
import com.fitpet.server.user.domain.entity.User;
import com.fitpet.server.user.domain.repository.UserRepository;
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
    private final UserRepository userRepository;

    public void saveTermsAgreements(Long userId, List<TermsAgreementCommand> commands) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        List<Terms> activeTerms = termsRepository.findAllActiveTerms(java.time.LocalDate.now());

        List<TermsAgreement> agreementsToSave = new ArrayList<>();

        for (TermsAgreementCommand command : commands) {
            Terms terms = activeTerms.stream()
                    .filter(t -> t.getId().equals(command.termsId()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(ErrorCode.TERMS_NOT_FOUND));

            if (terms.getCode().isRequired() && !command.isAgreed()) {
                throw new BusinessException(ErrorCode.REQUIRED_TERMS_NOT_AGREED);
            }

            agreementsToSave.add(TermsAgreement.builder()
                    .user(user)
                    .terms(terms)
                    .isAgreed(command.isAgreed())
                    .build());
        }

        termsAgreementRepository.saveAll(agreementsToSave);
    }
}