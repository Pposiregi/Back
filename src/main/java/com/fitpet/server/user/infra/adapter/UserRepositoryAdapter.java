package com.fitpet.server.user.infra.adapter;

import com.fitpet.server.user.domain.entity.Gender;
import com.fitpet.server.user.domain.entity.User;
import com.fitpet.server.user.domain.repository.UserRepository;
import com.fitpet.server.user.infra.jpa.UserJpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpa;

    @Override
    public User save(User user) {
        return jpa.save(user);
    }

    @Override
    public Optional<User> findById(Long id) {
        return jpa.findById(id);
    }


    @Override
    public Optional<User> findByEmail(String email) {
        return jpa.findByEmail(email);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpa.existsByEmail(email);
    }

    @Override
    public boolean existsByNickname(String nickname) {
        return jpa.existsByNickname(nickname);
    }

    @Override
    public boolean existsByEmailAndIdNot(String email, Long id) {
        return jpa.existsByEmailAndIdNot(email, id);
    }

    @Override
    public boolean existsByNicknameAndIdNot(String nickname, Long id) {
        return jpa.existsByNicknameAndIdNot(nickname, id);
    }

    @Override
    public void delete(User user) {
        if (user.getId() == null) {
            throw new IllegalArgumentException("삭제할 사용자가 저장되지 않았습니다.");
        }
        jpa.deleteById(user.getId());
    }

    @Override
    public Optional<User> findByProviderAndProviderUid(String provider, String providerUid) {
        return jpa.findByProviderAndProviderUid(provider, providerUid);
    }

    @Override
    public int resetDailyStepCount() {
        return jpa.resetDailyStepCount();
    }

    @Override
    public Page<User> findUsersBelowStepTarget(Pageable pageable) {
        return jpa.findUsersBelowStepTarget(pageable);
    }

    @Override
    public Page<User> findInactiveUsers(LocalDateTime inactiveSince, Pageable pageable) {
        return jpa.findInactiveUsers(inactiveSince, pageable);
    }

    @Override
    public List<User> findTopRankers(int limit) {
        return jpa.findTopRankers(PageRequest.of(0, limit));
    }

    @Override
    public long countByDailyStepCountGreaterThan(int dailyStepCount) {
        return jpa.countByDailyStepCountGreaterThan(dailyStepCount);
    }

    @Override
    public List<User> findTopRankersByGender(Gender gender, int limit) {
        return jpa.findTopRankersByGender(gender, PageRequest.of(0, limit));
    }
}
