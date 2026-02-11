package com.fitpet.server.report.application.service;

import com.fitpet.server.dailywalk.domain.repository.DailyWalkRepository;
import com.fitpet.server.dailyworkout.domain.entity.GpsSession;
import com.fitpet.server.dailyworkout.domain.repository.GpsSessionRepository;
import com.fitpet.server.meal.application.service.MealService;
import com.fitpet.server.meal.domain.entity.Meal;
import com.fitpet.server.meal.domain.repository.MealRepository;
import com.fitpet.server.meal.presentation.dto.response.MealDetailInfo;
import com.fitpet.server.meal.presentation.dto.response.MealDetailResponse;
import com.fitpet.server.report.application.mapper.ReportMapper;
import com.fitpet.server.report.presentation.dto.response.DailyMealSummaryResponse;
import com.fitpet.server.report.presentation.dto.response.DayInfo;
import com.fitpet.server.report.presentation.dto.response.MealCalendarResponse;
import com.fitpet.server.report.presentation.dto.response.ReportResponseDto.ActivityRangeResponse;
import com.fitpet.server.report.presentation.dto.response.ReportResponseDto.TodayActivityResponse;
import com.fitpet.server.shared.exception.BusinessException;
import com.fitpet.server.shared.exception.ErrorCode;
import com.fitpet.server.shared.s3.S3Service;
import com.fitpet.server.user.domain.entity.User;
import com.fitpet.server.user.domain.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final UserRepository userRepository;
    private final GpsSessionRepository gpsSessionRepository;
    private final DailyWalkRepository dailyWalkRepository;
    private final MealRepository mealRepository;

    private final S3Service s3Service;
    private final MealService mealService;

    private final ReportMapper reportMapper;


    @Override
    public TodayActivityResponse getTodayActivity(Long userId, LocalDate date) {
        User user = findUserById(userId);
        List<GpsSession> sessions = gpsSessionRepository.findByUserAndStartTimeBetween(user, date.atStartOfDay(),
                date.plusDays(1).atStartOfDay());

        BigDecimal totalDistance = sessions.stream()
                .map(GpsSession::getTotalDistance)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalCalories = sessions.stream()
                .filter(s -> s.getBurnCalories() != null)
                .mapToInt(GpsSession::getBurnCalories)
                .sum();

        return reportMapper.toTodayActivityResponse(sessions, date);
    }

    @Override
    public List<ActivityRangeResponse> getActivityRange(Long userId, LocalDate from, LocalDate to) {
        User user = findUserById(userId);

        return dailyWalkRepository.findByUserAndDateBetween(user, from, to).stream()
                .map(reportMapper::toActivityRangeResponse)
                .collect(Collectors.toList());
    }

    @Override
    public DailyMealSummaryResponse getDailyMealsReport(Long userId, LocalDate day) {
        User user = findUserById(userId);
        List<Meal> meals = mealRepository.findByUserAndDay(user, day);

        List<MealDetailInfo> mealList = meals.stream()
                .map(meal -> {
                    MealDetailInfo dto = reportMapper.toMealDetailInfo(meal);
                    String viewableUrl = s3Service.generatePresignedGetUrl(meal.getImageUrl());
                    dto.setImageUrl(viewableUrl);
                    return dto;
                })
                .collect(Collectors.toList());

        int totalKcal = mealList.stream()
                .filter(m -> m.getKcal() != null)
                .mapToInt(MealDetailInfo::getKcal)
                .sum();

        return reportMapper.toDailyMealSummaryResponse(day, totalKcal, mealList);
    }

    @Override
    public MealCalendarResponse getMealCalendarReport(Long userId, int year, int month) {
        User user = findUserById(userId);
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startOfMonth = yearMonth.atDay(1);
        LocalDate endOfMonth = yearMonth.atEndOfMonth();

        List<Meal> monthlyMeals = mealRepository.findByUserAndDayBetweenOrderByDayAsc(user, startOfMonth, endOfMonth);

        Map<LocalDate, List<Meal>> mealsByDate = monthlyMeals.stream()
                .collect(Collectors.groupingBy(Meal::getDay));

        List<DayInfo> daysInfo = startOfMonth.datesUntil(endOfMonth.plusDays(1))
                .map(date -> {
                    List<Meal> mealsOnDate = mealsByDate.getOrDefault(date, new ArrayList<>());

                    List<String> imageUrls = mealsOnDate.stream()
                            .map(meal -> s3Service.generatePresignedGetUrl(meal.getImageUrl()))
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());

                    DayInfo dayInfo = reportMapper.toDayInfo(date, mealsOnDate.size());
                    dayInfo.setImageUrls(imageUrls);
                    return dayInfo;
                })
                .filter(dayInfo -> dayInfo.getCount() > 0)
                .collect(Collectors.toList());

        return reportMapper.toMealCalendarResponse(year, month, daysInfo);
    }

    @Override
    public List<MealDetailResponse> getTodayMeals(Long userId, LocalDate date) {
        return mealService.getMealsByDate(userId, date);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}