package com.fitpet.server.dailyworkout.domain.repository;

import com.fitpet.server.dailyworkout.domain.entity.GpsSession;
import com.fitpet.server.user.domain.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


public interface GpsSessionRepository {
    GpsSession save(GpsSession gpsSession);

    List<GpsSession> findByUserAndStartTimeBetween(User user, LocalDateTime start, LocalDateTime end);

    List<GpsSession> findMonthlySessions(User user, LocalDateTime start, LocalDateTime end);

    Optional<GpsSession> findById(Long id);

    Optional<GpsSession> findActiveById(Long id);
}
