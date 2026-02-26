package com.fitpet.server.meal.application.service;

import com.fitpet.server.meal.application.dto.MealCreateCommand;
import com.fitpet.server.meal.application.dto.MealDetailInfo;
import com.fitpet.server.meal.application.dto.MealResult;
import com.fitpet.server.meal.application.dto.MealUpdateCommand;
import java.time.LocalDate;
import java.util.List;

public interface MealService {
    MealResult createMeal(Long userId, MealCreateCommand command);

    MealResult updateMeal(Long userId, Long mealId, MealUpdateCommand command);

    List<MealDetailInfo> getMealsByDate(Long userId, LocalDate day);

    void deleteMeal(Long userId, Long mealId);
}