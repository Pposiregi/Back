package com.fitpet.server.termsmaster.infra;

import com.fitpet.server.termsmaster.domain.entity.Terms;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TermsJpaRepository extends JpaRepository<Terms, Long> {

    // 현재 유효한 최신 버전 약관들 조회
    @Query("SELECT t FROM Terms t " +
            "WHERE t.effectiveDate <= :now " +
            "AND t.version = (" +
            "    SELECT MAX(t2.version) FROM Terms t2 " +
            "    WHERE t2.code = t.code AND t2.effectiveDate <= :now" +
            ")")
    List<Terms> findAllActiveTerms(@Param("now") LocalDate now);
}