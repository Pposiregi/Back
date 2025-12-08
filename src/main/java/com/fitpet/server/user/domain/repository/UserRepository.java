package com.fitpet.server.user.domain.repository;

import com.fitpet.server.user.domain.entity.Gender;
import com.fitpet.server.user.domain.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface UserRepository {
    User save(User user);

    Optional<User> findById(Long id);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByNicknameAndIdNot(String nickname, Long id);

    void delete(User user);

    Optional<User> findByEmail(String email);

    Optional<User> findByProviderAndProviderUid(String provider, String providerUid);

    int resetDailyStepCount();

    Page<User> findUsersBelowStepTarget(Pageable pageable);

    Page<User> findInactiveUsers(LocalDateTime inactiveSince, Pageable pageable);

    List<User> findTopRankers(int limit);

    List<User> findTopRankersByGender(Gender gender, int limit);

    long countByDailyStepCountGreaterThan(int dailyStepCount);

}
