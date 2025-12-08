package com.fitpet.server.user.application.service;

import com.fitpet.server.user.application.dto.GenderRankingResult;
import com.fitpet.server.user.application.dto.RankingResult;
import com.fitpet.server.user.domain.entity.Gender;
import com.fitpet.server.user.presentation.dto.UserCreateRequest;
import com.fitpet.server.user.presentation.dto.UserDto;
import com.fitpet.server.user.presentation.dto.UserInputInfoRequest;
import com.fitpet.server.user.presentation.dto.UserUpdateRequest;

public interface UserService {
    UserDto createUser(UserCreateRequest request);

    UserDto findUser(Long userId);

    UserDto updateUser(Long userId, UserUpdateRequest req);

    void deleteUser(Long userID);

    UserDto inputInfo(Long userId, UserInputInfoRequest request);

    boolean isRegistrationComplete(Long userId);

    RankingResult getDailyStepRanking(Long userId, int limit);

    GenderRankingResult getGenderDailyStepRanking(Gender gender, int limit);
}
