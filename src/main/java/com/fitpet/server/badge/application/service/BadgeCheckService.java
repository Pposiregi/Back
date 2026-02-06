package com.fitpet.server.badge.application.service;

import com.fitpet.server.badge.application.dto.BadgeCheckCreateCommand;
import com.fitpet.server.badge.application.dto.BadgeCheckResult;
import java.util.List;

public interface BadgeCheckService {
    BadgeCheckResult assignBadge(Long userId, BadgeCheckCreateCommand command);

    List<BadgeCheckResult> getUserBadges(Long userId);

    void revokeBadge(Long badgeCheckId);
}
