package com.fitpet.server.badge.application.service;

import com.fitpet.server.badge.application.dto.BadgeCreateCommand;
import com.fitpet.server.badge.application.dto.BadgeResult;
import com.fitpet.server.badge.application.dto.BadgeUpdateCommand;
import java.util.List;

public interface BadgeService {
    BadgeResult createBadge(BadgeCreateCommand command);

    BadgeResult getBadge(Long badgeId);

    List<BadgeResult> getBadges();

    BadgeResult updateBadge(Long badgeId, BadgeUpdateCommand command);

    void deleteBadge(Long badgeId);
}
