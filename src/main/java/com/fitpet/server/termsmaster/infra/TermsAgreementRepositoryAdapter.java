package com.fitpet.server.termsmaster.infra;

import com.fitpet.server.terms.domain.entity.TermsAgreement;
import com.fitpet.server.termsmaster.domain.repository.TermsAgreementRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TermsAgreementRepositoryAdapter implements TermsAgreementRepository {
    private final TermsAgreementJpaRepository jpaRepository;

    @Override
    public TermsAgreement save(TermsAgreement termsAgreement) {
        return jpaRepository.save(termsAgreement);
    }

    @Override
    public void saveAll(List<TermsAgreement> termsAgreements) {
        jpaRepository.saveAll(termsAgreements);
    }
}