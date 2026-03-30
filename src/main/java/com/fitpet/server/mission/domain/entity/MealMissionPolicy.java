package com.fitpet.server.mission.domain.entity;

import com.fitpet.server.meal.domain.entity.MealTime;

public enum MealMissionPolicy {
    BREAKFAST,
    LUNCH,
    DINNER,
    ANY_MEAL,
    THREE_MEALS;

    public boolean matches(MealTime mealTime, boolean firstMealOfTime) {
        if (mealTime == null || !firstMealOfTime) {
            return false;
        }

        return switch (this) {
            case BREAKFAST -> mealTime == MealTime.BREAKFAST;
            case LUNCH -> mealTime == MealTime.LUNCH;
            case DINNER -> mealTime == MealTime.DINNER;
            case ANY_MEAL, THREE_MEALS -> true;
        };
    }

    public static MealMissionPolicy infer(String title) {
        if (title == null || title.isBlank()) {
            return ANY_MEAL;
        }

        if (containsAny(title, "아침", "첫 끼", "첫끼")) {
            return BREAKFAST;
        }
        if (containsAny(title, "점심", "균형")) {
            return LUNCH;
        }
        if (containsAny(title, "저녁", "마무리", "마지막")) {
            return DINNER;
        }
        if (title.contains("세 끼")
                || title.contains("세끼")
                || title.contains("3끼")
                || title.contains("3 끼")) {
            return THREE_MEALS;
        }
        return ANY_MEAL;
    }

    private static boolean containsAny(String title, String... keywords) {
        String lower = title.toLowerCase();
        for (String keyword : keywords) {
            if (lower.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
