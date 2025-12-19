package com.fitpet.server.termsmaster.domain.repository;

import com.fitpet.server.terms.domain.entity.TermsAgreement;
import java.util.List;

public interface TermsAgreementRepository {
    TermsAgreement save(TermsAgreement termsAgreement);

    void saveAll(List<TermsAgreement> termsAgreements);
}