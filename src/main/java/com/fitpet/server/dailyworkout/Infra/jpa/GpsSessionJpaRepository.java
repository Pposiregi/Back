package com.fitpet.server.dailyworkout.Infra.jpa;

import com.fitpet.server.dailyworkout.domain.entity.GpsSession;
import com.fitpet.server.user.domain.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface GpsSessionJpaRepository extends JpaRepository<GpsSession, Long> {
    @Query("SELECT s FROM GpsSession s WHERE s.user = :user AND s.deleted = false "
        + "AND s.startTime >= :start AND s.startTime < :end")
    List<GpsSession> findByUserAndStartTimeBetween(@Param("user") User user,
                                                   @Param("start") LocalDateTime start,
                                                   @Param("end") LocalDateTime end);

    @Query("SELECT s FROM GpsSession s WHERE s.user = :user AND s.deleted = false "
        + "AND s.startTime >= :start AND s.startTime < :end "
        + "ORDER BY s.startTime DESC")
    List<GpsSession> findMonthlySessions(@Param("user") User user,
                                         @Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end);

    Optional<GpsSession> findByIdAndDeletedFalse(Long id);
}
