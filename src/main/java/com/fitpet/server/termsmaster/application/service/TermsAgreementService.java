package com.fitpet.server.termsmaster.application.service;

import com.fitpet.server.shared.exception.BusinessException;
import com.fitpet.server.shared.exception.ErrorCode;
import com.fitpet.server.termsmaster.application.dto.TermsAgreementCommand;
import com.fitpet.server.termsmaster.domain.entity.Terms;
import com.fitpet.server.termsmaster.domain.entity.TermsAgreement;
import com.fitpet.server.termsmaster.domain.repository.TermsAgreementRepository;
import com.fitpet.server.termsmaster.domain.repository.TermsRepository;
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

    public void saveTermsAgreements(Long userId, List<TermsAgreementCommand> commands) {
        List<TermsAgreementCommand> safeCommands = commands != null ? commands : List.of();
        doSave(userId, safeCommands);
    }

    private void doSave(Long userId, List<TermsAgreementCommand> commands) {
        Map<Long, Terms> activeTermsMap = termsRepository.findAllActiveTerms(LocalDate.now())
                .stream()
                .collect(Collectors.toMap(Terms::getId, t -> t));

        validateCommands(commands, activeTermsMap);

        Map<Long, TermsAgreement> existingMap = termsAgreementRepository.findAllByUserId(userId)
                .stream()
                .collect(Collectors.toMap(a -> a.getTerms().getId(), a -> a));

        List<TermsAgreement> agreementsToSave = commands.stream()
                .map(cmd -> resolveAgreement(cmd, userId, activeTermsMap, existingMap))
                .toList();

        termsAgreementRepository.saveAll(agreementsToSave);
        log.info("[TermsAgreementService] 약관 동의 저장 완료. userId={}, count={}", userId, commands.size());
    }

    private void validateCommands(List<TermsAgreementCommand> commands, Map<Long, Terms> activeTermsMap) {
        validateNoDuplicateTermsId(commands);
        commands.forEach(cmd -> findActiveTerms(cmd.termsId(), activeTermsMap));
        validateAllRequiredTermsIncluded(commands, activeTermsMap);
    }

    private TermsAgreement resolveAgreement(TermsAgreementCommand command, Long userId,
                                            Map<Long, Terms> activeTermsMap,
                                            Map<Long, TermsAgreement> existingMap) {
        Terms terms = findActiveTerms(command.termsId(), activeTermsMap);
        validateRequiredAgreement(terms, command.isAgreed());

        TermsAgreement existing = existingMap.get(terms.getId());
        if (existing != null) {
            existing.updateAgreed(command.isAgreed());
            return existing;
        }
        return TermsAgreement.builder()
                .userId(userId)
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
                                                  Map<Long, Terms> activeTermsMap) {
        Set<Long> submittedIds = commands.stream()
                .map(TermsAgreementCommand::termsId)
                .collect(Collectors.toSet());

        activeTermsMap.values().stream()
                .filter(t -> t.getCode().isRequired())
                .filter(t -> !submittedIds.contains(t.getId()))
                .findFirst()
                .ifPresent(t -> { throw new BusinessException(ErrorCode.REQUIRED_TERMS_NOT_AGREED); });
    }

    private Terms findActiveTerms(Long termsId, Map<Long, Terms> activeTermsMap) {
        Terms terms = activeTermsMap.get(termsId);
        if (terms == null) {
            throw new BusinessException(ErrorCode.TERMS_NOT_FOUND);
        }
        return terms;
    }

    private void validateRequiredAgreement(Terms terms, boolean isAgreed) {
        if (terms.getCode().isRequired() && !isAgreed) {
            throw new BusinessException(ErrorCode.REQUIRED_TERMS_NOT_AGREED);
        }
    }
}
