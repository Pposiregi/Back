package com.fitpet.server.meal.domain.repository;

import com.fitpet.server.meal.domain.entity.Meal;
import com.fitpet.server.user.domain.entity.User;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MealRepository {
    Meal save(Meal meal);

    Optional<Meal> findById(Long id);

    void delete(Meal meal);

    List<Meal> findByUserAndDay(User user, LocalDate day);

    List<Meal> findByUserAndDayBetweenOrderByDayAsc(User user, LocalDate startOfMonth, LocalDate endOfMonth);

    long countByUserAndDay(User user, LocalDate day);

    long countByUserAndDayAndSequence(User user, LocalDate day, Integer sequence);
}