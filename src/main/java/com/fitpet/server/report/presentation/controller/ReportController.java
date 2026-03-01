package com.fitpet.server.report.presentation.controller;

import com.fitpet.server.meal.application.dto.MealDetailInfo;
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

    @GetMapping("/activity/today")
    public ResponseEntity<TodayActivityResponse> getTodayActivity(
            @AuthUser Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(reportService.getTodayActivity(userId, date));
    }

    @GetMapping("/activity/range")
    public ResponseEntity<List<ActivityRangeResponse>> getActivityRange(
            @AuthUser Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(reportService.getActivityRange(userId, from, to));
    }

    @GetMapping("/meals/daily")
    public ResponseEntity<DailyMealSummaryResponse> getDailyMealsReport(
            @AuthUser Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(reportService.getDailyMealsReport(userId, date));
    }

    @GetMapping("/meals/calendar")
    public ResponseEntity<MealCalendarResponse> getMealCalendarReport(
            @AuthUser Long userId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        return ResponseEntity.ok(reportService.getMealCalendarReport(userId, year, month));
    }

    @GetMapping("/meals/today")
    public ResponseEntity<List<MealDetailResponse>> getTodayMeals(
            @AuthUser Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<MealDetailInfo> infos = reportService.getTodayMeals(userId, date);

        List<MealDetailResponse> response = infos.stream()
                .map(MealDetailResponse::from)
                .toList();

        return ResponseEntity.ok(response);
    }
}