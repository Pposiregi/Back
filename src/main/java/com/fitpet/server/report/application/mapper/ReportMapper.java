package com.fitpet.server.report.application.mapper;

import com.fitpet.server.dailywalk.domain.entity.DailyWalk;
import com.fitpet.server.dailyworkout.domain.entity.GpsSession;
import com.fitpet.server.meal.application.dto.MealDetailInfo;
import com.fitpet.server.meal.domain.entity.Meal;
import com.fitpet.server.report.presentation.dto.response.DailyMealSummaryResponse;
import com.fitpet.server.report.presentation.dto.response.DayInfo;
import com.fitpet.server.report.presentation.dto.response.MealCalendarResponse;
import com.fitpet.server.report.presentation.dto.response.ReportResponseDto.ActivityRangeResponse;
import com.fitpet.server.report.presentation.dto.response.ReportResponseDto.TodayActivityResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ReportMapper {

    @Mapping(source = "step", target = "steps")
    @Mapping(source = "burnCalories", target = "burnCalories")
    @Mapping(source = "distanceKm", target = "distanceKm")
    @Mapping(source = "createdAt", target = "date", qualifiedByName = "toLocalDate")
    ActivityRangeResponse toActivityRangeResponse(DailyWalk dailyWalk);

    @Mapping(source = "id", target = "mealId")
    @Mapping(target = "imageUrl", ignore = true)
    MealDetailInfo toMealDetailInfo(Meal meal);

    @Named("toLocalDate")
    default LocalDate toLocalDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.toLocalDate();
    }

    @Mapping(target = "date", source = "day")
    @Mapping(target = "totalKcal", source = "totalKcal")
    @Mapping(target = "mealList", source = "mealList")
    DailyMealSummaryResponse toDailyMealSummaryResponse(LocalDate day, Integer totalKcal,
                                                        List<MealDetailInfo> mealList);

    @Mapping(target = "date", source = "date")
    @Mapping(target = "distanceKm", source = "sessions", qualifiedByName = "sumDistance")
    @Mapping(target = "burnCalories", source = "sessions", qualifiedByName = "sumCalories")
    TodayActivityResponse toTodayActivityResponse(List<GpsSession> sessions, LocalDate date);


    @Mapping(target = "imageUrls", ignore = true)
    DayInfo toDayInfo(LocalDate date, int count);

    MealCalendarResponse toMealCalendarResponse(int year, int month, List<DayInfo> days);

    @Named("sumDistance")
    default BigDecimal sumDistance(List<GpsSession> sessions) {
        return sessions.stream()
                .map(GpsSession::getTotalDistance)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Named("sumCalories")
    default Integer sumCalories(List<GpsSession> sessions) {
        return sessions.stream()
                .filter(s -> s.getBurnCalories() != null)
                .mapToInt(GpsSession::getBurnCalories)
                .sum();
    }
}