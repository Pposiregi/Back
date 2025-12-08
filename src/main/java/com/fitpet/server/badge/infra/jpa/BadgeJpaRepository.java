package com.fitpet.server.badge.infra.jpa;

import com.fitpet.server.badge.domain.entity.Badge;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BadgeJpaRepository extends JpaRepository<Badge, Long> {
}
