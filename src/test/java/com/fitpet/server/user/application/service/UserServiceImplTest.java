package com.fitpet.server.user.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fitpet.server.pet.domain.repository.PetRepository;
import com.fitpet.server.shared.s3.S3Service;
import com.fitpet.server.user.application.dto.UserInputInfoCommand;
import com.fitpet.server.user.application.dto.UserResult;
import com.fitpet.server.user.application.dto.UserUpdateCommand;
import com.fitpet.server.user.application.mapper.UserMapper;
import com.fitpet.server.user.domain.entity.Gender;
import com.fitpet.server.user.domain.entity.RegistrationStatus;
import com.fitpet.server.user.domain.entity.User;
import com.fitpet.server.user.domain.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock PetRepository petRepository;
    @Mock UserMapper userMapper;
    @Mock PasswordEncoder passwordEncoder;
    @Mock S3Service s3Service;
    @Mock StringRedisTemplate redisTemplate;
    @Mock HashOperations<String, Object, Object> hashOperations;

    @InjectMocks UserServiceImpl sut;

    private static final Long USER_ID = 1L;
    private static final String USER_PROFILE_KEY = "user:profiles";

    @BeforeEach
    void setUp() {
        Mockito.lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
    }

    // ──────────────────────────────────────────────
    // inputInfo()
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("inputInfo 호출 시 Redis user:profiles 캐시가 새 닉네임으로 갱신된다")
    void inputInfo_Redis_캐시가_새_닉네임으로_갱신된다() {
        // given
        String newNickname = "새닉네임";
        User incompleteUser = User.builder()
                .id(USER_ID)
                .email("test@test.com")
                .registrationStatus(RegistrationStatus.INCOMPLETE)
                .build();
        User savedUser = User.builder()
                .id(USER_ID)
                .email("test@test.com")
                .nickname(newNickname)
                .registrationStatus(RegistrationStatus.COMPLETE)
                .build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(incompleteUser));
        when(userRepository.existsByNicknameAndIdNot(newNickname, USER_ID)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userMapper.toResult(savedUser)).thenReturn(UserResult.builder().userId(USER_ID).nickname(newNickname).build());

        UserInputInfoCommand command = new UserInputInfoCommand(
                newNickname, 25, Gender.male, 70.0, 175.0, 65.0, 20.0, 18.0, 8000
        );

        // when
        sut.inputInfo(USER_ID, command);

        // then
        verify(hashOperations).put(USER_PROFILE_KEY, String.valueOf(USER_ID), newNickname);
    }

    // ──────────────────────────────────────────────
    // updateUser()
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("updateUser 호출 시 닉네임이 있으면 Redis user:profiles 캐시가 갱신된다")
    void updateUser_닉네임_있으면_Redis_캐시가_갱신된다() {
        // given
        String newNickname = "수정닉네임";
        User existingUser = User.builder()
                .id(USER_ID)
                .email("test@test.com")
                .nickname("기존닉네임")
                .build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByNicknameAndIdNot(newNickname, USER_ID)).thenReturn(false);
        when(userMapper.toResult(any(User.class))).thenReturn(UserResult.builder().userId(USER_ID).build());

        UserUpdateCommand command = new UserUpdateCommand(
                null, null, newNickname, null, null, null, null, null, null, null, null
        );

        // when
        sut.updateUser(USER_ID, command);

        // then
        verify(hashOperations).put(USER_PROFILE_KEY, String.valueOf(USER_ID), newNickname);
    }

    @Test
    @DisplayName("updateUser 호출 시 닉네임이 null이면 Redis user:profiles 캐시를 갱신하지 않는다")
    void updateUser_닉네임_null이면_Redis_캐시_갱신_안_한다() {
        // given
        User existingUser = User.builder()
                .id(USER_ID)
                .email("test@test.com")
                .nickname("기존닉네임")
                .build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));
        when(userMapper.toResult(any(User.class))).thenReturn(UserResult.builder().userId(USER_ID).build());

        UserUpdateCommand command = new UserUpdateCommand(
                null, null, null, null, null, null, null, null, null, null, null
        );

        // when
        sut.updateUser(USER_ID, command);

        // then
        verify(hashOperations, never()).put(eq(USER_PROFILE_KEY), any(), any());
    }

    @Test
    @DisplayName("updateUser 호출 시 닉네임이 빈 문자열이면 Redis user:profiles 캐시를 갱신하지 않는다")
    void updateUser_닉네임_빈문자열이면_Redis_캐시_갱신_안_한다() {
        // given
        User existingUser = User.builder()
                .id(USER_ID)
                .email("test@test.com")
                .nickname("기존닉네임")
                .build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));
        when(userMapper.toResult(any(User.class))).thenReturn(UserResult.builder().userId(USER_ID).build());

        UserUpdateCommand command = new UserUpdateCommand(
                null, null, "", null, null, null, null, null, null, null, null
        );

        // when
        sut.updateUser(USER_ID, command);

        // then
        verify(hashOperations, never()).put(eq(USER_PROFILE_KEY), any(), any());
    }
}
