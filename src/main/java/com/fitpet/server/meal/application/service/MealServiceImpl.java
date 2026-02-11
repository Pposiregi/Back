package com.fitpet.server.meal.application.service;

import com.fitpet.server.meal.application.mapper.MealMapper;
import com.fitpet.server.meal.domain.entity.Meal;
import com.fitpet.server.meal.domain.entity.MealTime;
import com.fitpet.server.meal.domain.repository.MealRepository;
import com.fitpet.server.meal.presentation.dto.request.MealCreateRequest;
import com.fitpet.server.meal.presentation.dto.request.MealUpdateRequest;
import com.fitpet.server.meal.presentation.dto.response.MealCreateResponse;
import com.fitpet.server.meal.presentation.dto.response.MealDetailResponse;
import com.fitpet.server.meal.presentation.dto.response.MealUpdateResponse;
import com.fitpet.server.mission.application.service.MissionCheckService;
import com.fitpet.server.shared.exception.BusinessException;
import com.fitpet.server.shared.exception.ErrorCode;
import com.fitpet.server.shared.s3.S3Service;
import com.fitpet.server.shared.s3.type.ImageType;
import com.fitpet.server.user.domain.entity.User;
import com.fitpet.server.user.domain.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MealServiceImpl implements MealService {

    private final MealRepository mealRepository;
    private final UserRepository userRepository;
    private final MealMapper mealMapper;
    private final S3Service s3Service;
    private final MissionCheckService missionCheckService;

    @Override
    public MealCreateResponse createMeal(Long userId, MealCreateRequest request) {
        User user = findUserById(userId);
        Meal meal = mealMapper.toEntity(request, user);

        String imageKey = s3Service.createImageKey(userId, ImageType.MEAL);
        meal.setImageUrl(imageKey);

        Meal savedMeal = mealRepository.save(meal);
        missionCheckService.updateMealMissions(userId, meal.getDay(), MealTime.fromSequence(meal.getSequence()));

        String uploadUrl = s3Service.generatePresignedPutUrl(imageKey);
        return mealMapper.toCreateResponse(savedMeal, uploadUrl);
    }

    @Override
    public MealUpdateResponse updateMeal(Long userId, Long mealId, MealUpdateRequest request) {
        User user = findUserById(userId);
        Meal meal = findMealById(mealId);
        authorizeMealOwner(user, meal);

        if (request.getTitle() != null) {
            meal.setTitle(request.getTitle());
        }
        if (request.getKcal() != null) {
            meal.setKcal(request.getKcal());
        }
        if (request.getSequence() != null) {
            meal.setSequence(request.getSequence());
        }

        if (request.getChangeImage() != null && request.getChangeImage()) {
            if (meal.getImageUrl() != null && !meal.getImageUrl().isBlank()) {
                s3Service.deleteObject(meal.getImageUrl());
            }

            String newImageKey = s3Service.createImageKey(userId, ImageType.MEAL);
            meal.setImageUrl(newImageKey);
            String uploadUrl = s3Service.generatePresignedPutUrl(newImageKey);

            return mealMapper.toUpdateResponse(newImageKey, uploadUrl);
        } else {
            return MealUpdateResponse.builder().build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<MealDetailResponse> getMealsByDate(Long userId, LocalDate day) {
        User user = findUserById(userId);
        List<Meal> meals = mealRepository.findByUserAndDay(user, day);
        return meals.stream()
                .map(meal -> mealMapper.toMealResponse(meal, s3Service))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteMeal(Long userId, Long mealId) {
        User user = findUserById(userId);
        Meal meal = findMealById(mealId);
        authorizeMealOwner(user, meal);

        if (meal.getImageUrl() != null && !meal.getImageUrl().isBlank()) {
            s3Service.deleteObject(meal.getImageUrl());
        }
        mealRepository.delete(meal);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private Meal findMealById(Long mealId) {
        return mealRepository.findById(mealId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEAL_NOT_FOUND));
    }

    private void authorizeMealOwner(User user, Meal meal) {
        if (!meal.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.MEAL_ACCESS_DENIED);
        }
    }
}