package com.fitpet.server.user.application.service;

import com.fitpet.server.user.application.dto.ProfileImageUpdateResult;
import com.fitpet.server.user.application.dto.UserCreateCommand;
import com.fitpet.server.user.application.dto.UserInputInfoCommand;
import com.fitpet.server.user.application.dto.UserResult;
import com.fitpet.server.user.application.dto.UserUpdateCommand;

public interface UserService {
    UserResult createUser(UserCreateCommand command);

    UserResult findUser(Long userId);

    UserResult updateUser(Long userId, UserUpdateCommand command);

    ProfileImageUpdateResult updateProfileImage(Long userId);

    void deleteUser(Long userId);

    UserResult inputInfo(Long userId, UserInputInfoCommand command);

    boolean isRegistrationComplete(Long userId);

    void deleteProfileImage(Long userId);
}
