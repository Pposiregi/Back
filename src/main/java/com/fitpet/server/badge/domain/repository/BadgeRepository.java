package com.fitpet.server.badge.domain.repository;

import com.fitpet.server.badge.domain.entity.Badge;
import java.util.List;
import java.util.Optional;

public interface BadgeRepository {
    Badge save(Badge badge);
    Optional<Badge> findById(Long badgeId);
    List<Badge> findAll();
    void delete(Badge badge);
}
