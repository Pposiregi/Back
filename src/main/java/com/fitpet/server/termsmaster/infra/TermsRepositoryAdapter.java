package com.fitpet.server.termsmaster.infra;

import com.fitpet.server.termsmaster.domain.entity.Terms;
import com.fitpet.server.termsmaster.domain.entity.TermsType;
import com.fitpet.server.termsmaster.domain.repository.TermsRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TermsRepositoryAdapter implements TermsRepository {

    private final TermsJpaRepository jpaRepository;

    @Override
    public Terms save(Terms terms) {
        return jpaRepository.save(terms);
    }

    @Override
    public List<Terms> findAllActiveTerms(LocalDate date) {
        return jpaRepository.findAllActiveTerms(date);
    }

    @Override
    public Optional<Terms> findByCodeAndVersion(TermsType code, String version) {
        return jpaRepository.findByCodeAndVersion(code, version);
    }

    @Override
    public List<Terms> findAllByVersion(String version) {
        return jpaRepository.findAllByVersion(version);
    }
}