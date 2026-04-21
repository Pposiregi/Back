package com.fitpet.server.user.infra.jpa;

import com.fitpet.server.user.domain.entity.UserProfileImage;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface UserProfileImageJpaRepository extends JpaRepository<UserProfileImage, Long> {

    List<UserProfileImage> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserId(Long userId);

    @Query("SELECT p FROM UserProfileImage p WHERE p.userId = :userId ORDER BY p.createdAt ASC LIMIT 1")
    Optional<UserProfileImage> findOldestByUserId(@Param("userId") Long userId);

    // current 전환 시 동시성 보호 — 같은 유저의 concurrent 요청을 직렬화
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM UserProfileImage p WHERE p.userId = :userId AND p.current = true")
    Optional<UserProfileImage> findByUserIdAndCurrentTrue(@Param("userId") Long userId);

    Optional<UserProfileImage> findByUserIdAndImageKey(Long userId, String imageKey);

    @Modifying
    @Transactional
    void deleteByUserIdAndImageKey(Long userId, String imageKey);

    List<UserProfileImage> findAllByUserId(Long userId);

    @Modifying
    @Transactional
    void deleteAllByUserId(Long userId);
}
