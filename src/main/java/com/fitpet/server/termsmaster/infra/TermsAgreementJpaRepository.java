package com.fitpet.server.termsmaster.infra;

import com.fitpet.server.terms.domain.entity.TermsAgreement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TermsAgreementJpaRepository extends JpaRepository<TermsAgreement, Long> {
}