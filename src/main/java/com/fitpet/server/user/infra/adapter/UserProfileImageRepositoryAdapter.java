package com.fitpet.server.user.infra.adapter;

import com.fitpet.server.user.domain.entity.UserProfileImage;
import com.fitpet.server.user.domain.repository.UserProfileImageRepository;
import com.fitpet.server.user.infra.jpa.UserProfileImageJpaRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserProfileImageRepositoryAdapter implements UserProfileImageRepository {

    private final UserProfileImageJpaRepository jpaRepository;

    @Override
    public UserProfileImage save(UserProfileImage image) {
        return jpaRepository.save(image);
    }

    @Override
    public List<UserProfileImage> findTop10ByUserIdOrderByCreatedAtDesc(Long userId) {
        return jpaRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public long countByUserId(Long userId) {
        return jpaRepository.countByUserId(userId);
    }

    @Override
    public Optional<UserProfileImage> findOldestByUserId(Long userId) {
        return jpaRepository.findOldestByUserId(userId);
    }

    @Override
    public Optional<UserProfileImage> findCurrentByUserId(Long userId) {
        return jpaRepository.findByUserIdAndCurrentTrue(userId);
    }

    @Override
    public Optional<UserProfileImage> findByUserIdAndImageKey(Long userId, String imageKey) {
        return jpaRepository.findByUserIdAndImageKey(userId, imageKey);
    }

    @Override
    public void delete(UserProfileImage image) {
        jpaRepository.delete(image);
    }

    @Override
    public void deleteByUserIdAndImageKey(Long userId, String imageKey) {
        jpaRepository.deleteByUserIdAndImageKey(userId, imageKey);
    }
}
