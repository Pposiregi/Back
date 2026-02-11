package com.fitpet.server.user.application.service;

import com.fitpet.server.shared.s3.S3Service;
import com.fitpet.server.shared.s3.type.ImageType;
import com.fitpet.server.user.application.mapper.UserMapper;
import com.fitpet.server.user.domain.entity.RegistrationStatus;
import com.fitpet.server.user.domain.entity.User;
import com.fitpet.server.user.domain.exception.DuplicateEmailException;
import com.fitpet.server.user.domain.exception.DuplicateNicknameException;
import com.fitpet.server.user.domain.exception.UserNotFoundException;
import com.fitpet.server.user.domain.repository.UserRepository;
import com.fitpet.server.user.presentation.dto.UserDto;
import com.fitpet.server.user.presentation.dto.request.UserCreateRequest;
import com.fitpet.server.user.presentation.dto.request.UserInputInfoRequest;
import com.fitpet.server.user.presentation.dto.request.UserUpdateRequest;
import com.fitpet.server.user.presentation.dto.response.ProfileImageUpdateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
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
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final S3Service s3Service;
    private final StringRedisTemplate redisTemplate;

    private static final String USER_IMAGE_KEY = "user:images";

    @Override
    @Transactional
    public UserDto createUser(UserCreateRequest request) {
        validateUserCreateRequest(request);
        User user = userMapper.toEntity(request);
        user.changePassword(passwordEncoder.encode(request.password()));
        return userMapper.toDto(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto findUser(Long userId) {
        return userMapper.toDto(findUserById(userId));
    }

    @Override
    @Transactional
    public UserDto updateUser(Long userId, UserUpdateRequest request) {
        User user = findUserById(userId);
        validateUserUpdateRequest(userId, request);

        if (StringUtils.hasText(request.password())) {
            user.changePassword(passwordEncoder.encode(request.password()));
        }

        user.update(
                request.email(),
                request.nickname(),
                request.age(),
                request.gender(),
                request.weightKg(),
                request.targetWeightKg(),
                request.heightCm(),
                request.pbf(),
                request.targetPbf(),
                request.targetStepCount()
        );
        return userMapper.toDto(user);
    }

    @Override
    @Transactional
    public ProfileImageUpdateResponse updateProfileImage(Long userId) {
        User user = findUserById(userId);

        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isBlank()) {
            s3Service.deleteObject(user.getProfileImageUrl());
        }

        String newImageKey = s3Service.createImageKey(userId, ImageType.PROFILE);
        user.updateProfileImageUrl(newImageKey);

        redisTemplate.opsForHash().put(USER_IMAGE_KEY, String.valueOf(userId), newImageKey);

        String uploadUrl = s3Service.generatePresignedPutUrl(newImageKey);

        return new ProfileImageUpdateResponse(newImageKey, uploadUrl);
    }

    @Override
    @Transactional
    public void deleteProfileImage(Long userId) {
        User user = findUserById(userId);

        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isBlank()) {
            s3Service.deleteObject(user.getProfileImageUrl());
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
    public UserDto inputInfo(Long userId, UserInputInfoRequest request) {
        User user = findUserById(userId);

        if (user.getRegistrationStatus() == RegistrationStatus.COMPLETE) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "이미 가입이 완료된 사용자입니다.");
        }

        if (userRepository.existsByNicknameAndIdNot(request.nickname(), userId)) {
            throw new DuplicateNicknameException();
        }

        user.userInformation(request);
        return userMapper.toDto(userRepository.save(user));
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

    private void validateUserCreateRequest(UserCreateRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException();
        }
        if (userRepository.existsByNickname(request.nickname())) {
            throw new DuplicateNicknameException();
        }
    }

    private void validateUserUpdateRequest(Long userId, UserUpdateRequest request) {
        if (StringUtils.hasText(request.email()) &&
                userRepository.existsByEmailAndIdNot(request.email(), userId)) {
            throw new DuplicateEmailException();
        }
        if (StringUtils.hasText(request.nickname()) &&
                userRepository.existsByNicknameAndIdNot(request.nickname(), userId)) {
            throw new DuplicateNicknameException();
        }
    }
}