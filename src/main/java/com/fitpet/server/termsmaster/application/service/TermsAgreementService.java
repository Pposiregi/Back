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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TermsAgreementService {

    private final TermsRepository termsRepository;
    private final TermsAgreementRepository termsAgreementRepository;
    private final UserRepository userRepository;

    /**
     * TermsController 진입점: userId로 User를 직접 조회 후 처리
     */
    public void saveTermsAgreements(Long userId, List<TermsAgreementCommand> commands) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        doSave(user, commands);
    }

    /**
     * UserFacade 진입점: 이미 조회된 User 객체를 받아 이중 조회 방지
     */
    public void saveTermsAgreements(User user, List<TermsAgreementCommand> commands) {
        doSave(user, commands);
    }

    private void doSave(User user, List<TermsAgreementCommand> commands) {
        Long userId = user.getId();
        List<Terms> activeTerms = termsRepository.findAllActiveTerms(LocalDate.now());

        validateNoDuplicateTermsId(commands);
        commands.forEach(cmd -> findActiveTerms(cmd.termsId(), activeTerms));
        validateAllRequiredTermsIncluded(commands, activeTerms);

        Map<Long, TermsAgreement> existingMap = termsAgreementRepository.findAllByUserId(userId)
                .stream()
                .collect(Collectors.toMap(a -> a.getTerms().getId(), a -> a));

        List<TermsAgreement> agreementsToSave = commands.stream()
                .map(command -> resolveAgreement(command, user, activeTerms, existingMap))
                .toList();

        termsAgreementRepository.saveAll(agreementsToSave);
        log.info("[TermsAgreementService] 약관 동의 저장 완료. userId={}, count={}", userId, commands.size());
    }

    private TermsAgreement resolveAgreement(TermsAgreementCommand command, User user,
                                            List<Terms> activeTerms,
                                            Map<Long, TermsAgreement> existingMap) {
        Terms terms = findActiveTerms(command.termsId(), activeTerms);
        validateRequiredAgreement(terms, command.isAgreed());

        TermsAgreement existing = existingMap.get(terms.getId());
        if (existing != null) {
            existing.updateAgreed(command.isAgreed());
            return existing;
        }
        return TermsAgreement.builder()
                .user(user)
                .terms(terms)
                .isAgreed(command.isAgreed())
                .build();
    }

    private void validateNoDuplicateTermsId(List<TermsAgreementCommand> commands) {
        Set<Long> seen = new HashSet<>();
        commands.stream()
                .map(TermsAgreementCommand::termsId)
                .filter(id -> !seen.add(id))
                .findFirst()
                .ifPresent(id -> { throw new BusinessException(ErrorCode.INVALID_TERMS_REQUEST); });
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
