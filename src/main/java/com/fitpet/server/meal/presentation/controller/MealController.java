package com.fitpet.server.meal.presentation.controller;

import com.fitpet.server.meal.application.service.MealService;
import com.fitpet.server.meal.presentation.dto.request.MealCreateRequest;
import com.fitpet.server.meal.presentation.dto.request.MealUpdateRequest;
import com.fitpet.server.meal.presentation.dto.response.MealCreateResponse;
import com.fitpet.server.meal.presentation.dto.response.MealUpdateResponse;
import com.fitpet.server.shared.annotation.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/meal")
@RequiredArgsConstructor
public class MealController {

    private final MealService mealService;

    @PostMapping
    public ResponseEntity<MealCreateResponse> createMeal(
            @AuthUser Long userId,
            @Valid @RequestBody MealCreateRequest request) {
        MealCreateResponse response = mealService.createMeal(userId, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{mealId}")
    public ResponseEntity<Object> updateMeal(
            @AuthUser Long userId,
            @PathVariable Long mealId,
            @Valid @RequestBody MealUpdateRequest request) {

        MealUpdateResponse response = mealService.updateMeal(userId, mealId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{mealId}")
    public ResponseEntity<Void> deleteMeal(
            @AuthUser Long userId,
            @PathVariable Long mealId) {
        mealService.deleteMeal(userId, mealId);
        return ResponseEntity.noContent().build();
    }
}