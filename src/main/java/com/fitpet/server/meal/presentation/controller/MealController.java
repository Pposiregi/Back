package com.fitpet.server.meal.presentation.controller;

import com.fitpet.server.meal.application.dto.MealDetailInfo;
import com.fitpet.server.meal.application.dto.MealResult;
import com.fitpet.server.meal.application.service.MealService;
import com.fitpet.server.meal.presentation.dto.request.MealCreateRequest;
import com.fitpet.server.meal.presentation.dto.request.MealUpdateRequest;
import com.fitpet.server.meal.presentation.dto.response.MealCreateResponse;
import com.fitpet.server.meal.presentation.dto.response.MealDetailResponse;
import com.fitpet.server.meal.presentation.dto.response.MealUpdateResponse;
import com.fitpet.server.shared.annotation.AuthUser;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/meals")
@RequiredArgsConstructor
public class MealController {

    private final MealService mealService;

    @PostMapping
    public ResponseEntity<MealCreateResponse> createMeal(
            @AuthUser Long userId,
            @Valid @RequestBody MealCreateRequest request
    ) {
        MealResult result = mealService.createMeal(userId, request.toCommand());

        return ResponseEntity.ok(MealCreateResponse.from(result));
    }

    @PatchMapping("/{mealId}")
    public ResponseEntity<MealUpdateResponse> updateMeal(
            @AuthUser Long userId,
            @PathVariable Long mealId,
            @Valid @RequestBody MealUpdateRequest request
    ) {
        MealResult result = mealService.updateMeal(userId, mealId, request.toCommand());
        return ResponseEntity.ok(MealUpdateResponse.from(result));
    }

    @GetMapping
    public ResponseEntity<List<MealDetailResponse>> getMeals(
            @AuthUser Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate day
    ) {
        List<MealDetailInfo> infos = mealService.getMealsByDate(userId, day);

        List<MealDetailResponse> responses = infos.stream()
                .map(MealDetailResponse::from)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/{mealId}")
    public ResponseEntity<Void> deleteMeal(
            @AuthUser Long userId,
            @PathVariable Long mealId
    ) {
        mealService.deleteMeal(userId, mealId);
        return ResponseEntity.noContent().build();
    }
}