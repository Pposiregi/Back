package com.fitpet.server.badge.domain.repository;

import com.fitpet.server.badge.domain.entity.BadgeCheck;
import java.util.List;
import java.util.Optional;

public interface BadgeCheckRepository {
    BadgeCheck save(BadgeCheck badgeCheck);
    Optional<BadgeCheck> findById(Long badgeCheckId);
    Optional<BadgeCheck> findByUserIdAndBadgeId(Long userId, Long badgeId);
    List<BadgeCheck> findAllByUserId(Long userId);
    void delete(BadgeCheck badgeCheck);
}
