package com.fitpet.server.user.domain.repository;

import com.fitpet.server.user.domain.entity.UserProfileImageHistory;
import java.util.List;
import java.util.Optional;

public interface UserProfileImageHistoryRepository {
    void save(UserProfileImageHistory history);
    List<UserProfileImageHistory> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);
    long countByUserId(Long userId);
    Optional<UserProfileImageHistory> findOldestByUserId(Long userId);
    void delete(UserProfileImageHistory history);
    void deleteByUserIdAndImageKey(Long userId, String imageKey);
}
