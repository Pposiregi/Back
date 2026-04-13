package com.fitpet.server.termsmaster.domain.repository;

import com.fitpet.server.termsmaster.domain.entity.TermsAgreement;
import java.util.List;

public interface TermsAgreementRepository {
    TermsAgreement save(TermsAgreement termsAgreement);

    void saveAll(List<TermsAgreement> termsAgreements);

    List<TermsAgreement> findAllByUserId(Long userId);
}
