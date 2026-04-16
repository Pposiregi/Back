package com.fitpet.server.user.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fitpet.server.pet.domain.repository.PetRepository;
import com.fitpet.server.shared.s3.S3Service;
import com.fitpet.server.user.application.dto.ProfileImageHistoryResult;
import com.fitpet.server.user.application.mapper.UserMapper;
import com.fitpet.server.user.domain.entity.User;
import com.fitpet.server.user.domain.entity.UserProfileImageHistory;
import com.fitpet.server.user.domain.exception.ProfileImageAccessDeniedException;
import com.fitpet.server.user.domain.exception.ProfileImageNotFoundException;
import com.fitpet.server.user.domain.repository.UserProfileImageHistoryRepository;
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
class ProfileImageHistoryServiceTest {

    @Mock UserRepository userRepository;
    @Mock PetRepository petRepository;
    @Mock UserMapper userMapper;
    @Mock PasswordEncoder passwordEncoder;
    @Mock S3Service s3Service;
    @Mock StringRedisTemplate redisTemplate;
    @Mock HashOperations<String, Object, Object> hashOperations;
    @Mock RedisScript<Long> hsetWithExpireScript;
    @Mock UserProfileImageHistoryRepository profileImageHistoryRepository;

    @InjectMocks UserServiceImpl sut;

    private static final Long USER_ID = 1L;
    private static final String EXISTING_IMAGE_KEY = "user/1/profile/old-image.jpg";
    private static final String NEW_IMAGE_KEY = "user/1/profile/new-image.jpg";

    // ──────────────────────────────────────────────
    // updateProfileImage() — 이력 저장
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("updateProfileImage 호출 시 기존 이미지가 있으면 S3 삭제 없이 이력에 저장한다")
    void updateProfileImage_기존이미지_있으면_S3삭제_없이_이력저장() {
        // given
        User user = User.builder().id(USER_ID).profileImageUrl(EXISTING_IMAGE_KEY).build();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(s3Service.createImageKey(USER_ID, com.fitpet.server.shared.s3.type.ImageType.PROFILE))
                .thenReturn(NEW_IMAGE_KEY);
        when(s3Service.generatePresignedPutUrl(NEW_IMAGE_KEY)).thenReturn("https://s3.presigned/put");
        when(profileImageHistoryRepository.countByUserId(USER_ID)).thenReturn(0L);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        // when
        sut.updateProfileImage(USER_ID);

        // then — 기존 이미지 S3 삭제 안 함
        verify(s3Service, never()).deleteObject(EXISTING_IMAGE_KEY);
        // then — 기존 이미지 키를 이력에 저장
        verify(profileImageHistoryRepository).save(
                argThat(h -> h.getUserId().equals(USER_ID) && h.getImageKey().equals(EXISTING_IMAGE_KEY))
        );
    }

    @Test
    @DisplayName("updateProfileImage 호출 시 기존 이미지가 없으면 이력 저장을 시도하지 않는다")
    void updateProfileImage_기존이미지_없으면_이력저장_안함() {
        // given
        User user = User.builder().id(USER_ID).profileImageUrl(null).build();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(s3Service.createImageKey(USER_ID, com.fitpet.server.shared.s3.type.ImageType.PROFILE))
                .thenReturn(NEW_IMAGE_KEY);
        when(s3Service.generatePresignedPutUrl(NEW_IMAGE_KEY)).thenReturn("https://s3.presigned/put");
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        // when
        sut.updateProfileImage(USER_ID);

        // then
        verify(profileImageHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateProfileImage 호출 시 이력이 10개이면 가장 오래된 이미지를 S3+DB에서 삭제한다")
    void updateProfileImage_이력_10개이면_가장오래된것_삭제() {
        // given
        String oldestKey = "user/1/profile/oldest.jpg";
        UserProfileImageHistory oldest = UserProfileImageHistory.of(USER_ID, oldestKey);

        User user = User.builder().id(USER_ID).profileImageUrl(EXISTING_IMAGE_KEY).build();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(s3Service.createImageKey(USER_ID, com.fitpet.server.shared.s3.type.ImageType.PROFILE))
                .thenReturn(NEW_IMAGE_KEY);
        when(s3Service.generatePresignedPutUrl(NEW_IMAGE_KEY)).thenReturn("https://s3.presigned/put");
        when(profileImageHistoryRepository.countByUserId(USER_ID)).thenReturn(10L);
        when(profileImageHistoryRepository.findOldestByUserId(USER_ID)).thenReturn(Optional.of(oldest));
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        // when
        sut.updateProfileImage(USER_ID);

        // then
        verify(s3Service).deleteObject(oldestKey);
        verify(profileImageHistoryRepository).delete(oldest);
    }

    // ──────────────────────────────────────────────
    // checkHistoryImageAccess() — S3 검증 (non-TX)
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("checkHistoryImageAccess 호출 시 S3에 없는 키이면 ProfileImageNotFoundException 발생")
    void checkHistoryImageAccess_S3에_없는_키_예외() {
        // given
        String imageKey = "user/1/profile/not-exist.jpg";
        when(s3Service.doesObjectExist(imageKey)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> sut.checkHistoryImageAccess(USER_ID, imageKey))
                .isInstanceOf(ProfileImageNotFoundException.class);
    }

    @Test
    @DisplayName("checkHistoryImageAccess 호출 시 다른 유저의 이미지 키이면 ProfileImageAccessDeniedException 발생")
    void checkHistoryImageAccess_타인_이미지_키_예외() {
        // given
        String otherUserImageKey = "user/99/profile/other.jpg";

        // when & then
        assertThatThrownBy(() -> sut.checkHistoryImageAccess(USER_ID, otherUserImageKey))
                .isInstanceOf(ProfileImageAccessDeniedException.class);
    }

    // ──────────────────────────────────────────────
    // applyHistoryImage() — DB 업데이트 (TX)
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("applyHistoryImage 호출 시 user.profileImageUrl 업데이트 및 Redis 캐시 갱신")
    void applyHistoryImage_정상_케이스() {
        // given
        String imageKey = "user/1/profile/some-image.jpg";
        User user = User.builder().id(USER_ID).profileImageUrl(EXISTING_IMAGE_KEY).build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        // when
        sut.applyHistoryImage(USER_ID, imageKey);

        // then
        assertThat(user.getProfileImageUrl()).isEqualTo(imageKey);
        verify(hashOperations).put("user:images", String.valueOf(USER_ID), imageKey);
    }

    // ──────────────────────────────────────────────
    // getProfileImageHistory()
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("getProfileImageHistory 호출 시 이력 목록을 Presigned URL로 변환하여 반환한다")
    void getProfileImageHistory_이력_Presigned_URL_반환() {
        // given
        String key1 = "user/1/profile/img1.jpg";
        String key2 = "user/1/profile/img2.jpg";
        List<UserProfileImageHistory> histories = List.of(
                UserProfileImageHistory.of(USER_ID, key1),
                UserProfileImageHistory.of(USER_ID, key2)
        );
        when(profileImageHistoryRepository.findTop10ByUserIdOrderByCreatedAtDesc(USER_ID))
                .thenReturn(histories);
        when(s3Service.generatePresignedGetUrl(key1)).thenReturn("https://s3/presigned/img1");
        when(s3Service.generatePresignedGetUrl(key2)).thenReturn("https://s3/presigned/img2");

        // when
        List<ProfileImageHistoryResult> results = sut.getProfileImageHistory(USER_ID);

        // then
        assertThat(results).hasSize(2);
        assertThat(results.get(0).imageKey()).isEqualTo(key1);
        assertThat(results.get(0).presignedUrl()).isEqualTo("https://s3/presigned/img1");
        assertThat(results.get(1).imageKey()).isEqualTo(key2);
    }

    @Test
    @DisplayName("getProfileImageHistory 호출 시 이력이 없으면 빈 목록을 반환한다")
    void getProfileImageHistory_이력없으면_빈목록() {
        // given
        when(profileImageHistoryRepository.findTop10ByUserIdOrderByCreatedAtDesc(USER_ID))
                .thenReturn(List.of());

        // when
        List<ProfileImageHistoryResult> results = sut.getProfileImageHistory(USER_ID);

        // then
        assertThat(results).isEmpty();
    }
}
