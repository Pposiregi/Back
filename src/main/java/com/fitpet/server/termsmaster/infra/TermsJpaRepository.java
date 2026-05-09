package com.fitpet.server.termsmaster.infra;

import com.fitpet.server.termsmaster.domain.entity.Terms;
import com.fitpet.server.termsmaster.domain.entity.TermsType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TermsJpaRepository extends JpaRepository<Terms, Long> {

    // 현재 유효한 최신 버전 약관들 조회
    // effectiveDate가 같으면 createdAt이 가장 최근인 것 하나만 반환
    @Query("SELECT t FROM Terms t " +
            "WHERE t.effectiveDate <= :now " +
            "AND t.effectiveDate = (" +
            "    SELECT MAX(t2.effectiveDate) FROM Terms t2 " +
            "    WHERE t2.code = t.code AND t2.effectiveDate <= :now" +
            ") " +
            "AND t.createdAt = (" +
            "    SELECT MAX(t3.createdAt) FROM Terms t3 " +
            "    WHERE t3.code = t.code " +
            "    AND t3.effectiveDate = (" +
            "        SELECT MAX(t4.effectiveDate) FROM Terms t4 " +
            "        WHERE t4.code = t.code AND t4.effectiveDate <= :now" +
            "    )" +
            ")")
    List<Terms> findAllActiveTerms(@Param("now") LocalDate now);

    // 특정 버전의 모든 약관 조회
    List<Terms> findAllByVersion(String version);

    Optional<Terms> findByCodeAndVersion(TermsType code, String version);
}