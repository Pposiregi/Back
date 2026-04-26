package com.fitpet.server.user.application.service;

import com.fitpet.server.user.application.dto.ProfileImageHistoryResult;
import com.fitpet.server.user.application.dto.ProfileImageUpdateResult;
import com.fitpet.server.user.application.dto.UserCreateCommand;
import com.fitpet.server.user.application.dto.UserInputInfoCommand;
import com.fitpet.server.user.application.dto.UserResult;
import com.fitpet.server.user.application.dto.UserUpdateCommand;
import java.util.List;

public interface UserService {
    UserResult createUser(UserCreateCommand command);

    UserResult findUser(Long userId);

    UserResult updateUser(Long userId, UserUpdateCommand command);

    ProfileImageUpdateResult updateProfileImage(Long userId);

    void checkHistoryImageAccess(Long userId, String imageKey);

    void applyHistoryImage(Long userId, String imageKey);

    List<ProfileImageHistoryResult> getProfileImageHistory(Long userId);

    void withdrawUser(Long userId);

    UserResult inputInfo(Long userId, UserInputInfoCommand command);

    boolean isRegistrationComplete(Long userId);

    void deleteProfileImage(Long userId);
}
