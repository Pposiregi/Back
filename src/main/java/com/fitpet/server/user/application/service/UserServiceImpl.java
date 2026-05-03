package com.fitpet.server.user.application.service;

import com.fitpet.server.pet.domain.repository.PetRepository;
import com.fitpet.server.shared.exception.BusinessException;
import com.fitpet.server.shared.exception.ErrorCode;
import com.fitpet.server.shared.s3.S3Service;
import com.fitpet.server.shared.s3.type.ImageType;
import com.fitpet.server.user.application.dto.PetSummaryResult;
import com.fitpet.server.user.application.dto.ProfileImageUpdateResult;
import com.fitpet.server.user.application.dto.UserCreateCommand;
import com.fitpet.server.user.application.dto.UserInputInfoCommand;
import com.fitpet.server.user.application.dto.UserResult;
import com.fitpet.server.user.application.dto.UserUpdateCommand;
import com.fitpet.server.user.application.mapper.UserMapper;
import com.fitpet.server.user.domain.entity.RegistrationStatus;
import com.fitpet.server.user.domain.entity.User;
import com.fitpet.server.user.domain.exception.DuplicateEmailException;
import com.fitpet.server.user.domain.exception.DuplicateNicknameException;
import com.fitpet.server.user.domain.exception.UserNotFoundException;
import com.fitpet.server.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final S3Service s3Service;
    private final StringRedisTemplate redisTemplate;

    private static final String USER_IMAGE_KEY = "user:images";
    private static final String PROFILE_PRESET_PREFIX = "profile-presets/";

    @Override
    @Transactional
    public UserResult createUser(UserCreateCommand command) {
        validateUserCreateRequest(command);
        User user = userMapper.toEntity(command);
        user.changePassword(passwordEncoder.encode(command.password()));
        return userMapper.toResult(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResult findUser(Long userId) {
        User user = findUserById(userId);

        UserResult baseResult = userMapper.toResult(user);
        PetSummaryResult petSummary = petRepository.findByOwnerId(userId)
            .map(pet -> PetSummaryResult.builder()
                .petId(pet.getId())
                .name(pet.getName())
                .petType(pet.getPetType())
                .color(pet.getColor())
                .exp(pet.getExp())
                .expression(pet.getExpression())
                .build())
            .orElse(null);

        return enrichWithPresignedUrl(baseResult.withPet(petSummary), user.getProfileImageUrl());
    }

    private UserResult enrichWithPresignedUrl(UserResult result, String imageKey) {
        if (!StringUtils.hasText(imageKey)) {
            return result;
        }

        String presignedUrl = s3Service.generatePresignedGetUrl(imageKey);
        return result.withProfileImageUrl(presignedUrl);
    }

    @Override
    @Transactional
    public UserResult updateUser(Long userId, UserUpdateCommand command) {
        User user = findUserById(userId);
        validateUserUpdateRequest(userId, command);

        if (StringUtils.hasText(command.password())) {
            user.changePassword(passwordEncoder.encode(command.password()));
        }

        user.update(
            command.email(),
            command.nickname(),
            command.age(),
            command.gender(),
            command.weightKg(),
            command.targetWeightKg(),
            command.heightCm(),
            command.pbf(),
            command.targetPbf(),
            command.targetStepCount()
        );

        if (StringUtils.hasText(command.profileImageKey())) {
            updateProfileImageKey(userId, user, command.profileImageKey());
        }

        return enrichWithPresignedUrl(userMapper.toResult(user), user.getProfileImageUrl());
    }

    @Override
    @Transactional
    public ProfileImageUpdateResult updateProfileImage(Long userId) {
        User user = findUserById(userId);

        deleteOwnedProfileImageIfPresent(userId, user.getProfileImageUrl());

        String newImageKey = s3Service.createImageKey(userId, ImageType.PROFILE);
        user.updateProfileImageUrl(newImageKey);

        redisTemplate.opsForHash().put(USER_IMAGE_KEY, String.valueOf(userId), newImageKey);

        String uploadUrl = s3Service.generatePresignedPutUrl(newImageKey);

        return new ProfileImageUpdateResult(newImageKey, uploadUrl);
    }

    @Override
    @Transactional
    public void deleteProfileImage(Long userId) {
        User user = findUserById(userId);

        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isBlank()) {
            deleteOwnedProfileImageIfPresent(userId, user.getProfileImageUrl());
            user.updateProfileImageUrl(null);
            redisTemplate.opsForHash().delete(USER_IMAGE_KEY, String.valueOf(userId));
        }
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        User user = findUserById(userId);
        userRepository.delete(user);
    }

    @Override
    @Transactional
    public UserResult inputInfo(Long userId, UserInputInfoCommand command) {
        User user = findUserById(userId);

        if (user.getRegistrationStatus() == RegistrationStatus.COMPLETE) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "이미 가입이 완료된 사용자입니다.");
        }

        if (userRepository.existsByNicknameAndIdNot(command.nickname(), userId)) {
            throw new DuplicateNicknameException();
        }

        user.userInformation(
            command.nickname(),
            command.age(),
            command.gender(),
            command.weightKg(),
            command.heightCm(),
            command.targetWeightKg(),
            command.pbf(),
            command.targetPbf(),
            command.targetStepCount()
        );

        return userMapper.toResult(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isRegistrationComplete(Long userId) {
        User user = findUserById(userId);
        return user.getRegistrationStatus() == RegistrationStatus.COMPLETE;
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(UserNotFoundException::new);
    }

    private void validateUserCreateRequest(UserCreateCommand command) {
        if (userRepository.existsByEmail(command.email())) {
            throw new DuplicateEmailException();
        }
        if (userRepository.existsByNickname(command.nickname())) {
            throw new DuplicateNicknameException();
        }
    }

    private void validateUserUpdateRequest(Long userId, UserUpdateCommand command) {
        if (StringUtils.hasText(command.email()) &&
            userRepository.existsByEmailAndIdNot(command.email(), userId)) {
            throw new DuplicateEmailException();
        }
        if (StringUtils.hasText(command.nickname()) &&
            userRepository.existsByNicknameAndIdNot(command.nickname(), userId)) {
            throw new DuplicateNicknameException();
        }
    }

    private void updateProfileImageKey(Long userId, User user, String profileImageKey) {
        validateProfileImageKey(userId, profileImageKey);
        String previousProfileImageKey = user.getProfileImageUrl();
        user.updateProfileImageUrl(profileImageKey);
        redisTemplate.opsForHash().put(USER_IMAGE_KEY, String.valueOf(userId), profileImageKey);
        if (!profileImageKey.equals(previousProfileImageKey)) {
            deleteOwnedProfileImageIfPresent(userId, previousProfileImageKey);
        }
    }

    private void validateProfileImageKey(Long userId, String profileImageKey) {
        if (!isPresetProfileImage(profileImageKey) && !isOwnedProfileImage(userId, profileImageKey)) {
            throw new BusinessException(ErrorCode.USER_PROFILE_IMAGE_ACCESS_DENIED);
        }

        try {
            s3Service.existsObject(profileImageKey);
        } catch (S3Exception e) {
            throw new BusinessException(ErrorCode.USER_PROFILE_IMAGE_ACCESS_DENIED);
        }
    }

    private void deleteOwnedProfileImageIfPresent(Long userId, String profileImageKey) {
        if (isOwnedProfileImage(userId, profileImageKey)) {
            s3Service.deleteObject(profileImageKey);
        }
    }

    private boolean isPresetProfileImage(String profileImageKey) {
        return StringUtils.hasText(profileImageKey) && profileImageKey.startsWith(PROFILE_PRESET_PREFIX);
    }

    private boolean isOwnedProfileImage(Long userId, String profileImageKey) {
        if (!StringUtils.hasText(profileImageKey)) {
            return false;
        }
        return profileImageKey.startsWith("user/" + userId + "/" + ImageType.PROFILE.getPath() + "/");
    }
}
