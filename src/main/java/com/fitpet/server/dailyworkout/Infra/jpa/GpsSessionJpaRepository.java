package com.fitpet.server.dailyworkout.Infra.jpa;

import com.fitpet.server.dailyworkout.domain.entity.GpsSession;
import com.fitpet.server.user.domain.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface GpsSessionJpaRepository extends JpaRepository<GpsSession, Long> {
    List<GpsSession> findByUserAndStartTimeBetween(User user, LocalDateTime start, LocalDateTime end);

    @Query("SELECT s FROM GpsSession s WHERE s.user = :user AND s.startTime >= :start AND s.startTime < :end "
        + "ORDER BY s.startTime DESC")
    List<GpsSession> findMonthlySessions(@Param("user") User user,
                                         @Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end);
}