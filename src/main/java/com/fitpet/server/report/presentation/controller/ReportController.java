package com.fitpet.server.report.presentation.controller;

import com.fitpet.server.meal.presentation.dto.response.MealDetailResponse;
import com.fitpet.server.report.application.service.ReportService;
import com.fitpet.server.report.presentation.dto.response.DailyMealSummaryResponse;
import com.fitpet.server.report.presentation.dto.response.MealCalendarResponse;
import com.fitpet.server.report.presentation.dto.response.ReportResponseDto.ActivityRangeResponse;
import com.fitpet.server.report.presentation.dto.response.ReportResponseDto.TodayActivityResponse;
import com.fitpet.server.shared.annotation.AuthUser;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/activity/daily")
    public ResponseEntity<TodayActivityResponse> getTodayActivity(
            @AuthUser Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        TodayActivityResponse response = reportService.getTodayActivity(userId, date);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/activity/range")
    public ResponseEntity<List<ActivityRangeResponse>> getActivityRange(
            @AuthUser Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<ActivityRangeResponse> response = reportService.getActivityRange(userId, from, to);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/meals")
    public ResponseEntity<List<MealDetailResponse>> getTodayMeals(
            @AuthUser Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate day) {

        List<MealDetailResponse> response = reportService.getTodayMeals(userId, day);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/meal/day")
    public ResponseEntity<DailyMealSummaryResponse> getDailyMealsReport(
            @AuthUser Long userId,
            @RequestParam("day") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate day) {

        DailyMealSummaryResponse response = reportService.getDailyMealsReport(userId, day);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/meal/calendar")
    public ResponseEntity<MealCalendarResponse> getMealCalendarReport(
            @AuthUser Long userId,
            @RequestParam int year,
            @RequestParam int month) {

        MealCalendarResponse response = reportService.getMealCalendarReport(userId, year, month);
        return ResponseEntity.ok(response);
    }
}