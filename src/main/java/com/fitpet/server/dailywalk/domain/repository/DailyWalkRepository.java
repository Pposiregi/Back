package com.fitpet.server.dailywalk.domain.repository;

import com.fitpet.server.dailywalk.domain.entity.DailyWalk;
import com.fitpet.server.user.domain.entity.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DailyWalkRepository {
    List<DailyWalk> findAllByUser_Id(Long userId);

    Optional<DailyWalk> findByUser_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(Long userId,
                                                                                      LocalDateTime start,
                                                                                      LocalDateTime end);

    List<DailyWalk> findAllByUser_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(Long userId,
                                                                                     LocalDateTime start,
                                                                                     LocalDateTime end);

    DailyWalk save(DailyWalk dailyWalk);

    List<DailyWalk> findByUserAndDateBetween(User user, LocalDate start, LocalDate end);
    
    Optional<DailyWalk> findById(Long id);

    boolean existsById(Long id);

    void delete(DailyWalk dailyWalk);

    void deleteById(Long id);

    int updateStepByUserIdAndDate(Long userId,
                                  LocalDateTime start,
                                  LocalDateTime end,
                                  Integer step,
                                  BigDecimal distanceKm,
                                  Integer burnCalories
    );
}