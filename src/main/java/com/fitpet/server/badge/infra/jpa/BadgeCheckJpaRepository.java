package com.fitpet.server.badge.infra.jpa;

import com.fitpet.server.badge.domain.entity.BadgeCheck;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BadgeCheckJpaRepository extends JpaRepository<BadgeCheck, Long> {
    Optional<BadgeCheck> findByUser_IdAndBadge_Id(Long userId, Long badgeId);
    List<BadgeCheck> findAllByUser_IdOrderByCreatedAtDesc(Long userId);
}
