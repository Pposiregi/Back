package com.fitpet.server.termsmaster.infra;

import com.fitpet.server.termsmaster.domain.entity.TermsAgreement;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TermsAgreementJpaRepository extends JpaRepository<TermsAgreement, Long> {

    Optional<TermsAgreement> findByUserIdAndTermsId(Long userId, Long termsId);

    List<TermsAgreement> findAllByUserId(Long userId);
}