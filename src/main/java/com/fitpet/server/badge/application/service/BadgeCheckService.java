package com.fitpet.server.badge.application.service;

import com.fitpet.server.badge.presentation.dto.BadgeCheckCreateRequest;
import com.fitpet.server.badge.presentation.dto.BadgeCheckDto;
import java.util.List;

public interface BadgeCheckService {
    BadgeCheckDto assignBadge(Long userId, BadgeCheckCreateRequest request);

    List<BadgeCheckDto> getUserBadges(Long userId);

    void revokeBadge(Long badgeCheckId);
}
