package com.fitpet.server.dailywalk.infra.jpa;

import com.fitpet.server.dailywalk.domain.entity.DailyWalk;
import com.fitpet.server.dailywalk.domain.repository.DailyWalkRepository;
import com.fitpet.server.user.domain.entity.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DailyWalkRepositoryAdapter implements DailyWalkRepository {

    private final DailyWalkJpaRepository jpaRepository;

    @Override
    public List<DailyWalk> findAllByUser_Id(Long userId) {
        return jpaRepository.findAllByUser_Id(userId);
    }

    @Override
    public Optional<DailyWalk> findByUser_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(Long userId,
                                                                                             LocalDateTime start,
                                                                                             LocalDateTime end) {
        return jpaRepository.findByPeriod(userId, start, end);
    }

    @Override
    public List<DailyWalk> findAllByUser_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(Long userId,
                                                                                            LocalDateTime start,
                                                                                            LocalDateTime end) {
        return jpaRepository.findAllByPeriod(userId, start, end);
    }

    @Override
    public DailyWalk save(DailyWalk dailyWalk) {
        return jpaRepository.save(dailyWalk);
    }

    @Override
    public List<DailyWalk> findByUserAndDateBetween(User user, LocalDate start, LocalDate end) {
        return jpaRepository.findByUserAndDateBetween(user, start, end);
    }

    @Override
    public Optional<DailyWalk> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public void delete(DailyWalk dailyWalk) {
        jpaRepository.delete(dailyWalk);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public int updateStepByUserIdAndDate(Long userId,
                                         LocalDateTime start,
                                         LocalDateTime end,
                                         Integer step,
                                         BigDecimal distanceKm,
                                         Integer burnCalories) {
        return jpaRepository.updateStepByUserIdAndDate(userId, start, end, step, distanceKm, burnCalories);
    }
}