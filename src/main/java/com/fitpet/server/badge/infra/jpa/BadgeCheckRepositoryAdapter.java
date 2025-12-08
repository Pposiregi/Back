package com.fitpet.server.badge.infra.jpa;

import com.fitpet.server.badge.domain.entity.BadgeCheck;
import com.fitpet.server.badge.domain.repository.BadgeCheckRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BadgeCheckRepositoryAdapter implements BadgeCheckRepository {

    private final BadgeCheckJpaRepository badgeCheckJpaRepository;

    @Override
    public BadgeCheck save(BadgeCheck badgeCheck) {
        return badgeCheckJpaRepository.save(badgeCheck);
    }

    @Override
    public Optional<BadgeCheck> findById(Long badgeCheckId) {
        return badgeCheckJpaRepository.findById(badgeCheckId);
    }

    @Override
    public Optional<BadgeCheck> findByUserIdAndBadgeId(Long userId, Long badgeId) {
        return badgeCheckJpaRepository.findByUser_IdAndBadge_Id(userId, badgeId);
    }

    @Override
    public List<BadgeCheck> findAllByUserId(Long userId) {
        return badgeCheckJpaRepository.findAllByUser_IdOrderByCreatedAtDesc(userId);
    }

    @Override
    public void delete(BadgeCheck badgeCheck) {
        badgeCheckJpaRepository.delete(badgeCheck);
    }
}
