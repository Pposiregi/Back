package com.fitpet.server.user.infra.jpa;

import com.fitpet.server.user.domain.entity.UserProfileImageHistory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileImageHistoryJpaRepository extends JpaRepository<UserProfileImageHistory, Long> {
    List<UserProfileImageHistory> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);
    long countByUserId(Long userId);
    Optional<UserProfileImageHistory> findFirstByUserIdOrderByCreatedAtAsc(Long userId);
    void deleteByUserIdAndImageKey(Long userId, String imageKey);
}
