package com.fitpet.server.meal.infra.jpa;

import com.fitpet.server.meal.domain.entity.Meal;
import com.fitpet.server.user.domain.entity.User;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MealJpaRepository extends JpaRepository<Meal, Long> {
    List<Meal> findByUserAndDay(User user, LocalDate day);

    List<Meal> findByUserAndDayBetweenOrderByDayAsc(User user, LocalDate startOfMonth, LocalDate endOfMonth);

    long countByUserAndDay(User user, LocalDate day);

    long countByUserAndDayAndSequence(User user, LocalDate day, Integer sequence);
}