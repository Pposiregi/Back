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
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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

        List<Terms> activeTerms = termsRepository.findAllActiveTerms(LocalDate.now());

        commands.forEach(cmd -> findActiveTerms(cmd.termsId(), activeTerms));
        validateAllRequiredTermsIncluded(commands, activeTerms);

        List<TermsAgreement> agreementsToSave = commands.stream()
                .map(command -> resolveAgreement(command, user, activeTerms, userId))
                .toList();

        termsAgreementRepository.saveAll(agreementsToSave);
    }

    private TermsAgreement resolveAgreement(TermsAgreementCommand command, User user,
                                            List<Terms> activeTerms, Long userId) {
        Terms terms = findActiveTerms(command.termsId(), activeTerms);
        validateRequiredAgreement(terms, command.isAgreed());

        return termsAgreementRepository
                .findByUserIdAndTermsId(userId, terms.getId())
                .map(existing -> {
                    existing.updateAgreed(command.isAgreed());
                    return existing;
                })
                .orElse(TermsAgreement.builder()
                        .user(user)
                        .terms(terms)
                        .isAgreed(command.isAgreed())
                        .build());
    }

    private void validateAllRequiredTermsIncluded(List<TermsAgreementCommand> commands,
                                                  List<Terms> activeTerms) {
        Set<Long> submittedIds = commands.stream()
                .map(TermsAgreementCommand::termsId)
                .collect(Collectors.toSet());

        activeTerms.stream()
                .filter(t -> t.getCode().isRequired())
                .filter(t -> !submittedIds.contains(t.getId()))
                .findFirst()
                .ifPresent(t -> { throw new BusinessException(ErrorCode.REQUIRED_TERMS_NOT_AGREED); });
    }

    private Terms findActiveTerms(Long termsId, List<Terms> activeTerms) {
        return activeTerms.stream()
                .filter(t -> t.getId().equals(termsId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.TERMS_NOT_FOUND));
    }

    private void validateRequiredAgreement(Terms terms, boolean isAgreed) {
        if (terms.getCode().isRequired() && !isAgreed) {
            throw new BusinessException(ErrorCode.REQUIRED_TERMS_NOT_AGREED);
        }
    }
}