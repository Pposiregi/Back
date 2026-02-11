package com.fitpet.server.user.application.service;

import com.fitpet.server.user.presentation.dto.UserDto;
import com.fitpet.server.user.presentation.dto.request.UserCreateRequest;
import com.fitpet.server.user.presentation.dto.request.UserInputInfoRequest;
import com.fitpet.server.user.presentation.dto.request.UserUpdateRequest;
import com.fitpet.server.user.presentation.dto.response.ProfileImageUpdateResponse;

public interface UserService {
    UserDto createUser(UserCreateRequest request);

    UserDto findUser(Long userId);

    UserDto updateUser(Long userId, UserUpdateRequest request);

    ProfileImageUpdateResponse updateProfileImage(Long userId);

    void deleteUser(Long userId);

    UserDto inputInfo(Long userId, UserInputInfoRequest request);

    boolean isRegistrationComplete(Long userId);
}