package com.fitpet.server.termsmaster.infra;

import com.fitpet.server.termsmaster.domain.entity.TermsAgreement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TermsAgreementJpaRepository extends JpaRepository<TermsAgreement, Long> {

    List<TermsAgreement> findAllByUserId(Long userId);
}
