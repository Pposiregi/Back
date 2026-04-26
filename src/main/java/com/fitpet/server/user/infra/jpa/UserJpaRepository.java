package com.fitpet.server.user.infra.jpa;

import com.fitpet.server.user.domain.entity.Gender;
import com.fitpet.server.user.domain.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserJpaRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickName);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByNicknameAndIdNot(String nickname, Long id);

    Optional<User> findByEmail(String email);

    Optional<User> findByProviderAndProviderUid(String provider, String providerUid);

    @Query(value = "SELECT * FROM users WHERE email = :email LIMIT 1", nativeQuery = true)
    Optional<User> findByEmailIncludeDeleted(@Param("email") String email);

    @Query(value = "SELECT * FROM users WHERE provider = :provider AND provider_uid = :providerUid LIMIT 1", nativeQuery = true)
    Optional<User> findByOAuthIncludeDeleted(@Param("provider") String provider, @Param("providerUid") String providerUid);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update User user set user.dailyStepCount = 0")
    int resetDailyStepCount();

    @Query("SELECT u FROM User u " +
            "WHERE u.targetStepCount IS NOT NULL AND u.targetStepCount > 0 " +
            "AND u.dailyStepCount < (u.targetStepCount * 0.5) " +
            "AND u.deviceToken IS NOT NULL AND u.deviceToken != '' " +
            "AND u.allowActivityNotification = true")
    Page<User> findUsersBelowStepTarget(Pageable pageable);

    @Query("SELECT u FROM User u " +
            "WHERE u.lastAccessedAt < :inactiveSince " +
            "AND u.deviceToken IS NOT NULL AND u.deviceToken != '' " +
            "AND u.allowActivityNotification = true")
    Page<User> findInactiveUsers(
            @Param("inactiveSince") LocalDateTime inactiveSince,
            Pageable pageable
    );

    @Query("SELECT u FROM User u ORDER BY u.dailyStepCount DESC, u.updatedAt ASC")
    List<User> findTopRankers(Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.gender = :gender ORDER BY u.dailyStepCount DESC, u.updatedAt ASC")
    List<User> findTopRankersByGender(@Param("gender") Gender gender, Pageable pageable);

    long countByDailyStepCountGreaterThan(int dailyStepCount);

}
