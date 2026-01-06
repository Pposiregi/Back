package com.fitpet.server.meal.domain.entity;

import java.time.LocalTime;

public enum MealTime {
    BREAKFAST(1),
    LUNCH(2),
    DINNER(3);

    private final int sequence;

    MealTime(int sequence) {
        this.sequence = sequence;
    }

    public int getSequence() {
        return sequence;
    }

    public static MealTime from(LocalTime time) {
        if (time.isBefore(LocalTime.of(11, 0))) {
            return BREAKFAST;
        }
        if (time.isBefore(LocalTime.of(17, 0))) {
            return LUNCH;
        }
        return DINNER;
    }
}
