package com.fitpet.server.meal.infra.jpa;

import com.fitpet.server.meal.domain.entity.Meal;
import com.fitpet.server.meal.domain.repository.MealRepository;
import com.fitpet.server.user.domain.entity.User;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MealRepositoryAdapter implements MealRepository {
    private final MealJpaRepository mealJpaRepository;

    @Override
    public Meal save(Meal meal) {
        return mealJpaRepository.save(meal);
    }

    @Override
    public Optional<Meal> findById(Long id) {
        return mealJpaRepository.findById(id);
    }

    @Override
    public void delete(Meal meal) {
        mealJpaRepository.delete(meal);
    }

    @Override
    public List<Meal> findByUserAndDay(User user, LocalDate day) {
        return mealJpaRepository.findByUserAndDay(user, day);
    }

    @Override
    public List<Meal> findByUserAndDayBetweenOrderByDayAsc(User user, LocalDate startOfMonth, LocalDate endOfMonth) {
        return mealJpaRepository.findByUserAndDayBetweenOrderByDayAsc(user, startOfMonth, endOfMonth);
    }

    @Override
    public long countByUserAndDay(User user, LocalDate day) {
        return mealJpaRepository.countByUserAndDay(user, day);
    }

    @Override
    public long countByUserAndDayAndSequence(User user, LocalDate day, Integer sequence) {
        return mealJpaRepository.countByUserAndDayAndSequence(user, day, sequence);
    }
}