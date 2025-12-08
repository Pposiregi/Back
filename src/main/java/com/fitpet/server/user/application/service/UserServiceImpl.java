package com.fitpet.server.user.application.service;

import com.fitpet.server.user.application.dto.GenderRankingResult;
import com.fitpet.server.user.application.dto.RankingResult;
import com.fitpet.server.user.application.dto.UserRanking;
import com.fitpet.server.user.application.mapper.UserMapper;
import com.fitpet.server.user.domain.entity.Gender;
import com.fitpet.server.user.domain.entity.RegistrationStatus;
import com.fitpet.server.user.domain.entity.User;
import com.fitpet.server.user.domain.exception.DuplicateEmailException;
import com.fitpet.server.user.domain.exception.DuplicateNicknameException;
import com.fitpet.server.user.domain.exception.UserNotFoundException;
import com.fitpet.server.user.domain.repository.UserRepository;
import com.fitpet.server.user.presentation.dto.UserCreateRequest;
import com.fitpet.server.user.presentation.dto.UserDto;
import com.fitpet.server.user.presentation.dto.UserInputInfoRequest;
import com.fitpet.server.user.presentation.dto.UserUpdateRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Override
    @Transactional
    public UserDto createUser(UserCreateRequest request) {
        log.debug("회원가입 요청 email={}, nickname={}", request.email(), request.nickname());

        validateUserCreateRequest(request);

        User user = userMapper.toEntity(request);
        user.changePassword(passwordEncoder.encode(request.password()));
        User saved = userRepository.save(user);
        return userMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto findUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        return userMapper.toDto(user);
    }

    @Override
    @Transactional
    public UserDto updateUser(Long userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

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
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        userRepository.delete(user);
    }

    @Override
    @Transactional
    public UserDto inputInfo(Long userId, UserInputInfoRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        if (user.getRegistrationStatus() == RegistrationStatus.COMPLETE) {
            log.warn("사용자 정보 입력 실패 - 이미 가입 완료된 사용자: id: {}", userId);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "이미 가입이 완료된 사용자입니다.");
        }

        if (userRepository.existsByNicknameAndIdNot(request.nickname(), userId)) {
            log.warn("사용자 정보 입력 실패 - 닉네임 중복: {} (요청자 id: {})", maskNickname(request.nickname()), userId);
            throw new DuplicateNicknameException();
        }

        user.userInformation(request);

        userRepository.save(user);

        return userMapper.toDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isRegistrationComplete(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        return user.getRegistrationStatus() == RegistrationStatus.COMPLETE;
    }


    @Override
    @Transactional(readOnly = true)
    public RankingResult getDailyStepRanking(Long targetUserId, int limit) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(UserNotFoundException::new);

        int myStepCount = user.getDailyStepCount() == null ? 0 : user.getDailyStepCount();

        List<UserRanking> top10 = userRepository.findTopRankers(limit)
                .stream()
                .map(u -> new UserRanking(
                        u.getId(),
                        u.getNickname(),
                        u.getDailyStepCount() == null ? 0 : u.getDailyStepCount()
                ))
                .toList();

        long higherCount = userRepository.countByDailyStepCountGreaterThan(myStepCount);
        int myRank = (int) higherCount + 1;

        return new RankingResult(top10, myRank);
    }

    @Override
    @Transactional(readOnly = true)
    public GenderRankingResult getGenderDailyStepRanking(Gender gender, int limit) {

        List<UserRanking> top10 = userRepository.findTopRankersByGender(gender, limit)
                .stream()
                .map(u -> new UserRanking(
                        u.getId(),
                        u.getNickname(),
                        u.getDailyStepCount() == null ? 0 : u.getDailyStepCount()
                ))
                .toList();

        return new GenderRankingResult(top10);
    }

    private void validateUserCreateRequest(UserCreateRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            log.warn("회원가입 실패 - 중복 이메일: {}", maskEmail(request.email()));
            throw new DuplicateEmailException();
        }
        if (userRepository.existsByNickname(request.nickname())) {
            log.warn("회원가입 실패 - 중복 닉네임: {}", maskNickname(request.nickname()));
            throw new DuplicateNicknameException();
        }
    }

    private void validateUserUpdateRequest(Long userId, UserUpdateRequest request) {
        if (StringUtils.hasText(request.email()) &&
                userRepository.existsByEmailAndIdNot(request.email(), userId)) {
            log.warn("사용자 수정 실패 - 이메일 중복: {} (요청자 id: {})", maskEmail(request.email()), userId);
            throw new DuplicateEmailException();
        }
        if (StringUtils.hasText(request.nickname()) &&
                userRepository.existsByNicknameAndIdNot(request.nickname(), userId)) {
            log.warn("사용자 수정 실패 - 닉네임 중복: {} (요청자 id: {})", maskNickname(request.nickname()), userId);
            throw new DuplicateNicknameException();
        }
    }

    private static String maskEmail(String email) {
        if (email == null) {
            return null;
        }
        int at = email.indexOf('@');
        if (at <= 1) {
            return "***";
        }
        String local = email.substring(0, at);
        String domain = email.substring(at);
        String prefix = local.substring(0, Math.min(2, local.length()));
        return prefix + "***" + domain;
    }

    private static String maskNickname(String nickname) {
        if (nickname == null || nickname.isEmpty()) {
            return nickname;
        }
        return nickname.substring(0, 1) + "***";
    }
}
