package com.fitpet.server.user.infra.jpa;

import com.fitpet.server.user.domain.entity.UserProfileImage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserProfileImageJpaRepository extends JpaRepository<UserProfileImage, Long> {

    List<UserProfileImage> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserId(Long userId);

    @Query("SELECT p FROM UserProfileImage p WHERE p.userId = :userId ORDER BY p.createdAt ASC LIMIT 1")
    Optional<UserProfileImage> findOldestByUserId(@Param("userId") Long userId);

    Optional<UserProfileImage> findByUserIdAndCurrentTrue(Long userId);

    Optional<UserProfileImage> findByUserIdAndImageKey(Long userId, String imageKey);

    void deleteByUserIdAndImageKey(Long userId, String imageKey);
}
