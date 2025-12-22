package com.fitpet.server.dailyworkout.Infra.jpa;

import com.fitpet.server.dailyworkout.domain.entity.GpsSession;
import com.fitpet.server.dailyworkout.domain.repository.GpsSessionRepository;
import com.fitpet.server.user.domain.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class GpsSessionRepositoryAdapter implements GpsSessionRepository {

    private final GpsSessionJpaRepository gpsSessionJpaRepository;

    @Override
    public GpsSession save(GpsSession gpsSession) {
        return gpsSessionJpaRepository.save(gpsSession);
    }

    @Override
    public List<GpsSession> findByUserAndStartTimeBetween(User user, LocalDateTime start, LocalDateTime end) {
        return gpsSessionJpaRepository.findByUserAndStartTimeBetween(user, start, end);
    }

    @Override
    public List<GpsSession> findMonthlySessions(User user, LocalDateTime start, LocalDateTime end) {
        return gpsSessionJpaRepository.findMonthlySessions(user, start, end);
    }

    @Override
    public Optional<GpsSession> findById(Long id) {
        return gpsSessionJpaRepository.findById(id);
    }
}