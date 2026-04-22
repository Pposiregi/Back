package com.fitpet.server.user.application.service;

import com.fitpet.server.pet.domain.repository.PetRepository;
import com.fitpet.server.shared.s3.S3Service;
import com.fitpet.server.shared.s3.type.ImageType;
import com.fitpet.server.user.application.dto.PetSummaryResult;
import com.fitpet.server.user.application.dto.ProfileImageHistoryResult;
import com.fitpet.server.user.application.dto.ProfileImageUpdateResult;
import com.fitpet.server.user.application.dto.UserCreateCommand;
import com.fitpet.server.user.application.dto.UserInputInfoCommand;
import com.fitpet.server.user.application.dto.UserResult;
import com.fitpet.server.user.application.dto.UserUpdateCommand;
import com.fitpet.server.user.application.mapper.UserMapper;
import com.fitpet.server.user.domain.entity.RegistrationStatus;
import com.fitpet.server.user.domain.entity.User;
import com.fitpet.server.user.domain.entity.UserProfileImage;
import com.fitpet.server.user.domain.exception.DuplicateEmailException;
import com.fitpet.server.user.domain.exception.DuplicateNicknameException;
import com.fitpet.server.user.domain.exception.ProfileImageAccessDeniedException;
import com.fitpet.server.user.domain.exception.ProfileImageNotFoundException;
import com.fitpet.server.user.domain.exception.UserNotFoundException;
import com.fitpet.server.user.domain.repository.UserProfileImageRepository;
import com.fitpet.server.user.domain.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

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
    @Qualifier("hsetWithExpireScript")
    private final RedisScript<Long> hsetWithExpireScript;
    private final UserProfileImageRepository profileImageRepository;

    private static final String USER_IMAGE_KEY = "user:images";
    private static final String USER_PROFILE_KEY = "user:profiles";
    private static final String USER_PROFILE_TTL_SECONDS = "259200"; // 3일
    private static final int MAX_PROFILE_IMAGE_HISTORY = 10;

    @Override
    @Transactional
    public UserResult createUser(UserCreateCommand command) {
        return userRepository.findByEmailIncludeDeleted(command.email())
                .filter(u -> u.getDeletedAt() != null)
                .map(u -> reactivateUser(u, command.password()))
                .orElseGet(() -> {
                    validateUserCreateRequest(command);
                    User user = userMapper.toEntity(command);
                    user.changePassword(passwordEncoder.encode(command.password()));
                    return userMapper.toResult(userRepository.save(user));
                });
    }

    private UserResult reactivateUser(User user, String newPassword) {
        user.changePassword(passwordEncoder.encode(newPassword));
        user.reactivate();
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

        if (StringUtils.hasText(command.nickname())) {
            putUserProfileCache(userId, command.nickname());
        }

        if (StringUtils.hasText(command.profileImageKey())) {
            switchCurrentProfileImage(userId, command.profileImageKey());
            user.updateProfileImageUrl(command.profileImageKey());
            redisTemplate.opsForHash().put(USER_IMAGE_KEY, String.valueOf(userId), command.profileImageKey());
        }

        return userMapper.toResult(user);
    }

    @Override
    @Transactional
    public ProfileImageUpdateResult updateProfileImage(Long userId) {
        User user = findUserById(userId);

        profileImageRepository.findCurrentByUserId(userId).ifPresent(current -> {
            evictOldestIfFull(userId);
            current.deactivate();
        });

        String newImageKey = s3Service.createImageKey(userId, ImageType.PROFILE);
        profileImageRepository.save(UserProfileImage.createCurrent(userId, newImageKey));
        user.updateProfileImageUrl(newImageKey);

        redisTemplate.opsForHash().put(USER_IMAGE_KEY, String.valueOf(userId), newImageKey);

        String uploadUrl = s3Service.generatePresignedPutUrl(newImageKey);

        return new ProfileImageUpdateResult(newImageKey, uploadUrl);
    }

    @Override
    public void checkHistoryImageAccess(Long userId, String imageKey) {
        validateImageOwnership(userId, imageKey);
        if (!s3Service.doesObjectExist(imageKey)) {
            throw new ProfileImageNotFoundException();
        }
    }

    @Override
    @Transactional
    public void applyHistoryImage(Long userId, String imageKey) {
        User user = findUserById(userId);
        switchCurrentProfileImage(userId, imageKey);
        user.updateProfileImageUrl(imageKey);
        redisTemplate.opsForHash().put(USER_IMAGE_KEY, String.valueOf(userId), imageKey);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfileImageHistoryResult> getProfileImageHistory(Long userId) {
        return profileImageRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(img -> new ProfileImageHistoryResult(
                        img.getImageKey(),
                        s3Service.generatePresignedGetUrl(img.getImageKey()),
                        img.isCurrent()
                ))
                .toList();
    }

    @Override
    @Transactional
    public void deleteProfileImage(Long userId) {
        User user = findUserById(userId);

        profileImageRepository.findCurrentByUserId(userId).ifPresent(current -> {
            s3Service.deleteObject(current.getImageKey());
            profileImageRepository.delete(current);
        });

        user.updateProfileImageUrl(null);
        redisTemplate.opsForHash().delete(USER_IMAGE_KEY, String.valueOf(userId));
    }

    @Override
    public void cleanupUserImages(Long userId) {
        profileImageRepository.findAllByUserId(userId)
                .forEach(img -> s3Service.deleteObject(img.getImageKey()));
    }

    @Override
    @Transactional
    public void withdrawUser(Long userId) {
        User user = findUserById(userId);
        profileImageRepository.deleteAllByUserId(userId);

        redisTemplate.opsForHash().delete(USER_IMAGE_KEY, String.valueOf(userId));
        redisTemplate.opsForHash().delete(USER_PROFILE_KEY, String.valueOf(userId));

        user.withdraw();
        userRepository.save(user);
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

        User saved = userRepository.save(user);
        putUserProfileCache(userId, saved.getNickname());
        return userMapper.toResult(saved);
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

    /**
     * 지정 imageKey를 is_current=true로 전환하고 기존 current를 deactivate한다.
     * imageKey 레코드가 없으면 새로 생성한다.
     */
    private void switchCurrentProfileImage(Long userId, String imageKey) {
        profileImageRepository.findCurrentByUserId(userId)
                .ifPresent(UserProfileImage::deactivate);

        profileImageRepository.findByUserIdAndImageKey(userId, imageKey)
                .ifPresentOrElse(
                        UserProfileImage::activate,
                        () -> profileImageRepository.save(UserProfileImage.createCurrent(userId, imageKey))
                );
    }

    private void evictOldestIfFull(Long userId) {
        if (profileImageRepository.countByUserId(userId) >= MAX_PROFILE_IMAGE_HISTORY) {
            profileImageRepository.findOldestByUserId(userId).ifPresent(oldest -> {
                s3Service.deleteObject(oldest.getImageKey());
                profileImageRepository.delete(oldest);
            });
        }
    }

    private void validateImageOwnership(Long userId, String imageKey) {
        String expectedPrefix = "user/" + userId + "/profile/";
        if (imageKey == null || !imageKey.startsWith(expectedPrefix)) {
            throw new ProfileImageAccessDeniedException();
        }
    }

    private void putUserProfileCache(Long userId, String nickname) {
        redisTemplate.execute(
                hsetWithExpireScript,
                List.of(USER_PROFILE_KEY),
                String.valueOf(userId),
                nickname,
                USER_PROFILE_TTL_SECONDS
        );
    }
}
