package com.fitpet.server.termsmaster.domain.repository;

import com.fitpet.server.termsmaster.domain.entity.TermsAgreement;
import java.util.List;
import java.util.Optional;

public interface TermsAgreementRepository {
    TermsAgreement save(TermsAgreement termsAgreement);

    void saveAll(List<TermsAgreement> termsAgreements);

    Optional<TermsAgreement> findByUserIdAndTermsId(Long userId, Long termsId);
}