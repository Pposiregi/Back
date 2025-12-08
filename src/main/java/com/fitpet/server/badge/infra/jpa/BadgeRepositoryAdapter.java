package com.fitpet.server.badge.infra.jpa;

import com.fitpet.server.badge.domain.entity.Badge;
import com.fitpet.server.badge.domain.repository.BadgeRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BadgeRepositoryAdapter implements BadgeRepository {

    private final BadgeJpaRepository badgeJpaRepository;

    @Override
    public Badge save(Badge badge) {
        return badgeJpaRepository.save(badge);
    }

    @Override
    public Optional<Badge> findById(Long badgeId) {
        return badgeJpaRepository.findById(badgeId);
    }

    @Override
    public List<Badge> findAll() {
        return badgeJpaRepository.findAll();
    }

    @Override
    public void delete(Badge badge) {
        badgeJpaRepository.delete(badge);
    }
}
