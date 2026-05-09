package com.fitpet.server.user.domain.repository;

import com.fitpet.server.user.domain.entity.UserProfileImage;
import java.util.List;
import java.util.Optional;

public interface UserProfileImageRepository {
    UserProfileImage save(UserProfileImage image);
    List<UserProfileImage> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);
    long countByUserId(Long userId);
    Optional<UserProfileImage> findOldestByUserId(Long userId);
    Optional<UserProfileImage> findCurrentByUserId(Long userId);
    Optional<UserProfileImage> findByUserIdAndImageKey(Long userId, String imageKey);
    void delete(UserProfileImage image);
    void deleteByUserIdAndImageKey(Long userId, String imageKey);
    List<UserProfileImage> findAllByUserId(Long userId);
    void deleteAllByUserId(Long userId);
}
