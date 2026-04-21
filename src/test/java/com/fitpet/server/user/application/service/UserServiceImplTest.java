package com.fitpet.server.user.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fitpet.server.pet.domain.repository.PetRepository;
import com.fitpet.server.shared.exception.BusinessException;
import com.fitpet.server.shared.s3.S3Service;
import com.fitpet.server.user.application.dto.UserInputInfoCommand;
import com.fitpet.server.user.application.dto.UserResult;
import com.fitpet.server.user.application.dto.UserUpdateCommand;
import com.fitpet.server.user.application.mapper.UserMapper;
import com.fitpet.server.user.domain.entity.Gender;
import com.fitpet.server.user.domain.entity.RegistrationStatus;
import com.fitpet.server.user.domain.entity.User;
import com.fitpet.server.user.domain.entity.UserProfileImage;
import com.fitpet.server.user.domain.exception.UserNotFoundException;
import com.fitpet.server.user.domain.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock PetRepository petRepository;
    @Mock UserMapper userMapper;
    @Mock PasswordEncoder passwordEncoder;
    @Mock S3Service s3Service;
    @Mock StringRedisTemplate redisTemplate;
    @Mock RedisScript<Long> hsetWithExpireScript;
    @Mock com.fitpet.server.user.domain.repository.UserProfileImageRepository profileImageRepository;

    @InjectMocks UserServiceImpl sut;

    private static final Long USER_ID = 1L;
    private static final String USER_PROFILE_KEY = "user:profiles";
    private static final String TTL = "259200";

    // ──────────────────────────────────────────────
    // inputInfo()
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("inputInfo 호출 시 Lua 스크립트로 user:profiles 캐시가 닉네임+TTL 포함해 원자적으로 갱신된다")
    void inputInfo_Lua스크립트로_캐시_닉네임과_TTL_원자적_갱신() {
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

        // then — Lua 스크립트가 KEY, userId, nickname, TTL 로 호출됐는지 검증
        verify(redisTemplate).execute(
                eq(hsetWithExpireScript),
                eq(List.of(USER_PROFILE_KEY)),
                eq(String.valueOf(USER_ID)),
                eq(newNickname),
                eq(TTL)
        );
    }

    // ──────────────────────────────────────────────
    // updateUser()
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("updateUser 호출 시 닉네임이 있으면 Lua 스크립트로 user:profiles 캐시가 갱신된다")
    void updateUser_닉네임_있으면_Lua스크립트로_캐시_갱신() {
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
                null, null, newNickname, null, null, null, null, null, null, null, null, null
        );

        // when
        sut.updateUser(USER_ID, command);

        // then
        verify(redisTemplate).execute(
                eq(hsetWithExpireScript),
                eq(List.of(USER_PROFILE_KEY)),
                eq(String.valueOf(USER_ID)),
                eq(newNickname),
                eq(TTL)
        );
    }

    @Test
    @DisplayName("updateUser 호출 시 닉네임이 null이면 Lua 스크립트를 호출하지 않는다")
    void updateUser_닉네임_null이면_Lua스크립트_호출_안_한다() {
        // given
        User existingUser = User.builder()
                .id(USER_ID)
                .email("test@test.com")
                .nickname("기존닉네임")
                .build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));
        when(userMapper.toResult(any(User.class))).thenReturn(UserResult.builder().userId(USER_ID).build());

        UserUpdateCommand command = new UserUpdateCommand(
                null, null, null, null, null, null, null, null, null, null, null, null
        );

        // when
        sut.updateUser(USER_ID, command);

        // then
        verify(redisTemplate, never()).execute(any(RedisScript.class), anyList(), any());
    }

    // ──────────────────────────────────────────────
    // withdrawUser()
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("withdrawUser 호출 시 존재하지 않는 userId면 UserNotFoundException")
    void withdrawUser_존재하지_않는_userId면_예외() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.withdrawUser(999L))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("withdrawUser 호출 시 User 익명화 + deletedAt 설정 + save 호출")
    void withdrawUser_User_익명화_및_deletedAt_설정() {
        User user = User.builder().id(USER_ID).email("test@test.com").nickname("테스트").build();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_dummy");
        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOps);

        sut.withdrawUser(USER_ID);

        assertThat(user.getDeletedAt()).isNotNull();
        assertThat(user.getEmail()).startsWith("deleted_");
        assertThat(user.getNickname()).startsWith("deleted_");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("withdrawUser 호출 시 이미지 DB 전체 삭제 (S3 삭제는 cleanupUserImages 담당)")
    void withdrawUser_이미지_DB_삭제() {
        User user = User.builder().id(USER_ID).email("test@test.com").build();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_dummy");
        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOps);

        sut.withdrawUser(USER_ID);

        verify(profileImageRepository).deleteAllByUserId(USER_ID);
        verify(s3Service, never()).deleteObject(anyString());
    }

    @Test
    @DisplayName("cleanupUserImages 호출 시 모든 프로필 이미지 S3 삭제")
    void cleanupUserImages_S3_삭제() {
        UserProfileImage img1 = UserProfileImage.createCurrent(USER_ID, "key1");
        UserProfileImage img2 = UserProfileImage.createCurrent(USER_ID, "key2");
        when(profileImageRepository.findAllByUserId(USER_ID)).thenReturn(List.of(img1, img2));

        sut.cleanupUserImages(USER_ID);

        verify(s3Service).deleteObject("key1");
        verify(s3Service).deleteObject("key2");
    }

    @Test
    @DisplayName("withdrawUser 호출 시 Redis user:profiles, user:images 캐시 삭제")
    void withdrawUser_Redis_캐시_삭제() {
        User user = User.builder().id(USER_ID).email("test@test.com").build();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_dummy");
        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOps);

        sut.withdrawUser(USER_ID);

        verify(hashOps).delete("user:profiles", String.valueOf(USER_ID));
        verify(hashOps).delete("user:images", String.valueOf(USER_ID));
    }

    @Test
    @DisplayName("updateUser 호출 시 닉네임이 빈 문자열이면 Lua 스크립트를 호출하지 않는다")
    void updateUser_닉네임_빈문자열이면_Lua스크립트_호출_안_한다() {
        // given
        User existingUser = User.builder()
                .id(USER_ID)
                .email("test@test.com")
                .nickname("기존닉네임")
                .build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));
        when(userMapper.toResult(any(User.class))).thenReturn(UserResult.builder().userId(USER_ID).build());

        UserUpdateCommand command = new UserUpdateCommand(
                null, null, "", null, null, null, null, null, null, null, null, null
        );

        // when
        sut.updateUser(USER_ID, command);

        // then
        verify(redisTemplate, never()).execute(any(RedisScript.class), anyList(), any());
    }
}
