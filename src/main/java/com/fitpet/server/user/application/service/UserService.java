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

    // S3 존재 확인 + 소유권 검증 (트랜잭션 밖에서 호출)
    void checkHistoryImageAccess(Long userId, String imageKey);

    // DB 업데이트만 담당 (트랜잭션)
    void applyHistoryImage(Long userId, String imageKey);

    List<ProfileImageHistoryResult> getProfileImageHistory(Long userId);

    void withdrawUser(Long userId);

    UserResult inputInfo(Long userId, UserInputInfoCommand command);

    boolean isRegistrationComplete(Long userId);

    void deleteProfileImage(Long userId);
}
