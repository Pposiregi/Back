package com.fitpet.server.termsmaster.domain.repository;

import com.fitpet.server.termsmaster.domain.entity.Terms;
import com.fitpet.server.termsmaster.domain.entity.TermsType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TermsRepository {
    Terms save(Terms terms);

    List<Terms> findAllActiveTerms(LocalDate date);

    Optional<Terms> findByCodeAndVersion(TermsType code, String version);
}