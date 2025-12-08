package com.fitpet.server.badge.application.service;

import com.fitpet.server.badge.presentation.dto.BadgeCreateRequest;
import com.fitpet.server.badge.presentation.dto.BadgeDto;
import com.fitpet.server.badge.presentation.dto.BadgeUpdateRequest;
import java.util.List;

public interface BadgeService {
    BadgeDto createBadge(BadgeCreateRequest request);

    BadgeDto getBadge(Long badgeId);

    List<BadgeDto> getBadges();

    BadgeDto updateBadge(Long badgeId, BadgeUpdateRequest request);

    void deleteBadge(Long badgeId);
}
