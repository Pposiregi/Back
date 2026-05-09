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
import com.fitpet.server.shared.s3.S3Service;
import com.fitpet.server.user.application.dto.UserCreateCommand;
import com.fitpet.server.user.application.dto.UserInputInfoCommand;
import com.fitpet.server.user.application.dto.UserResult;
import com.fitpet.server.user.application.dto.UserUpdateCommand;
import com.fitpet.server.user.application.mapper.UserMapper;
import com.fitpet.server.user.domain.entity.Gender;
import com.fitpet.server.user.domain.entity.RegistrationStatus;
import com.fitpet.server.user.domain.entity.User;
import com.fitpet.server.user.domain.exception.DuplicateEmailException;
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

    // inputInfo()

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

    // updateUser()

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

    // withdrawUser()

    @Test
    @DisplayName("withdrawUser 호출 시 존재하지 않는 userId면 UserNotFoundException")
    void withdrawUser_존재하지_않는_userId면_예외() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.withdrawUser(999L))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("withdrawUser 호출 시 deletedAt만 설정되고 email/nickname은 변경되지 않는다")
    void withdrawUser_deletedAt만_설정() {
        User user = User.builder().id(USER_ID).email("test@test.com").nickname("테스트").build();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOps);

        sut.withdrawUser(USER_ID);

        assertThat(user.getDeletedAt()).isNotNull();
        assertThat(user.getEmail()).isEqualTo("test@test.com");
        assertThat(user.getNickname()).isEqualTo("테스트");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("withdrawUser 호출 시 이미지 DB 전체 삭제 (S3 삭제는 cleanupUserImages 담당)")
    void withdrawUser_이미지_DB_삭제() {
        User user = User.builder().id(USER_ID).email("test@test.com").build();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOps);

        sut.withdrawUser(USER_ID);

        verify(profileImageRepository).deleteAllByUserId(USER_ID);
        verify(s3Service, never()).deleteObject(anyString());
    }

    // createUser() — 재가입 재활성화

    @Test
    @DisplayName("createUser 호출 시 탈퇴한 이메일이면 기존 계정을 재활성화한다")
    void createUser_탈퇴_이메일_재활성화() {
        User deleted = User.builder()
                .id(USER_ID).email("test@test.com").nickname("기존닉네임")
                .build();
        deleted.withdraw();

        when(userRepository.findByEmailIncludeDeleted("test@test.com")).thenReturn(Optional.of(deleted));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any())).thenReturn(deleted);
        when(userMapper.toResult(any())).thenReturn(UserResult.builder().userId(USER_ID).build());

        UserCreateCommand command = new UserCreateCommand("test@test.com", "pass", "닉네임", null, null, null, null, null, null, null, null);
        sut.createUser(command);

        assertThat(deleted.getDeletedAt()).isNull();
        verify(userRepository).save(deleted);
    }

    @Test
    @DisplayName("createUser 호출 시 탈퇴 이력 없는 이메일이면 신규 가입한다")
    void createUser_탈퇴_이력_없으면_신규_가입() {
        when(userRepository.findByEmailIncludeDeleted("new@test.com")).thenReturn(Optional.empty());
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(userRepository.existsByNickname("새닉네임")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        User newUser = User.builder().id(2L).email("new@test.com").build();
        when(userMapper.toEntity(any())).thenReturn(newUser);
        when(userRepository.save(any())).thenReturn(newUser);
        when(userMapper.toResult(any())).thenReturn(UserResult.builder().userId(2L).build());

        UserCreateCommand command = new UserCreateCommand("new@test.com", "pass", "새닉네임", null, null, null, null, null, null, null, null);
        sut.createUser(command);

        verify(userRepository).save(newUser);
    }

    @Test
    @DisplayName("withdrawUser 호출 시 profileImageUrl과 deviceToken이 null이 된다")
    void withdrawUser_profileImageUrl_deviceToken_null화() {
        User user = User.builder()
                .id(USER_ID)
                .email("test@test.com")
                .profileImageUrl("s3://bucket/profile.jpg")
                .deviceToken("fcm-token")
                .build();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOps);

        sut.withdrawUser(USER_ID);

        assertThat(user.getProfileImageUrl()).isNull();
        assertThat(user.getDeviceToken()).isNull();
    }

    @Test
    @DisplayName("withdrawUser 호출 시 Redis user:profiles, user:images, user:genders 캐시 삭제")
    void withdrawUser_Redis_캐시_삭제() {
        User user = User.builder().id(USER_ID).email("test@test.com").build();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOps);

        sut.withdrawUser(USER_ID);

        verify(hashOps).delete("user:profiles", String.valueOf(USER_ID));
        verify(hashOps).delete("user:images", String.valueOf(USER_ID));
        verify(hashOps).delete("user:genders", String.valueOf(USER_ID));
    }

    @Test
    @DisplayName("createUser 호출 시 활성 계정(deletedAt==null)과 이메일 중복이면 DuplicateEmailException")
    void createUser_활성_계정_이메일_중복이면_예외() {
        User activeUser = User.builder().id(USER_ID).email("exist@test.com").build();
        // deletedAt == null → filter 통과 못함 → orElseGet → validateUserCreateRequest
        when(userRepository.findByEmailIncludeDeleted("exist@test.com")).thenReturn(Optional.of(activeUser));
        when(userRepository.existsByEmail("exist@test.com")).thenReturn(true);

        UserCreateCommand command = new UserCreateCommand("exist@test.com", "pass", "닉네임", null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> sut.createUser(command))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    @DisplayName("withdrawUser 호출 시 user.withdraw()가 호출되어 deletedAt이 설정된다")
    void withdrawUser_호출_시_deletedAt_설정() {
        User user = User.builder().id(USER_ID).email("test@test.com").build();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOps);

        sut.withdrawUser(USER_ID);

        assertThat(user.getDeletedAt()).isNotNull();
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
