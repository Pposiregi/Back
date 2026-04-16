package com.fitpet.server.user.infra.adapter;

import com.fitpet.server.user.domain.entity.UserProfileImageHistory;
import com.fitpet.server.user.domain.repository.UserProfileImageHistoryRepository;
import com.fitpet.server.user.infra.jpa.UserProfileImageHistoryJpaRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserProfileImageHistoryRepositoryAdapter implements UserProfileImageHistoryRepository {

    private final UserProfileImageHistoryJpaRepository jpaRepository;

    @Override
    public void save(UserProfileImageHistory history) {
        jpaRepository.save(history);
    }

    @Override
    public List<UserProfileImageHistory> findTop10ByUserIdOrderByCreatedAtDesc(Long userId) {
        return jpaRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public long countByUserId(Long userId) {
        return jpaRepository.countByUserId(userId);
    }

    @Override
    public Optional<UserProfileImageHistory> findOldestByUserId(Long userId) {
        return jpaRepository.findFirstByUserIdOrderByCreatedAtAsc(userId);
    }

    @Override
    public void delete(UserProfileImageHistory history) {
        jpaRepository.delete(history);
    }
}
