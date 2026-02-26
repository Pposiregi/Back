package com.fitpet.server.report.application.service;

import com.fitpet.server.meal.application.dto.MealDetailInfo;
import com.fitpet.server.report.presentation.dto.response.DailyMealSummaryResponse;
import com.fitpet.server.report.presentation.dto.response.MealCalendarResponse;
import com.fitpet.server.report.presentation.dto.response.ReportResponseDto.ActivityRangeResponse;
import com.fitpet.server.report.presentation.dto.response.ReportResponseDto.TodayActivityResponse;
import java.time.LocalDate;
import java.util.List;

public interface ReportService {

    TodayActivityResponse getTodayActivity(Long userId, LocalDate date);

    List<ActivityRangeResponse> getActivityRange(Long userId, LocalDate from, LocalDate to);

    DailyMealSummaryResponse getDailyMealsReport(Long userId, LocalDate day);

    MealCalendarResponse getMealCalendarReport(Long userId, int year, int month);

    List<MealDetailInfo> getTodayMeals(Long userId, LocalDate date);
}