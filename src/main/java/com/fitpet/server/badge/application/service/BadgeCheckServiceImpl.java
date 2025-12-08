package com.fitpet.server.badge.application.service;

import com.fitpet.server.badge.application.mapper.BadgeCheckMapper;
import com.fitpet.server.badge.domain.entity.Badge;
import com.fitpet.server.badge.domain.entity.BadgeCheck;
import com.fitpet.server.badge.domain.exception.BadgeCheckNotFoundException;
import com.fitpet.server.badge.domain.exception.BadgeNotFoundException;
import com.fitpet.server.badge.domain.repository.BadgeCheckRepository;
import com.fitpet.server.badge.domain.repository.BadgeRepository;
import com.fitpet.server.badge.presentation.dto.BadgeCheckCreateRequest;
import com.fitpet.server.badge.presentation.dto.BadgeCheckDto;
import com.fitpet.server.user.domain.entity.User;
import com.fitpet.server.user.domain.exception.UserNotFoundException;
import com.fitpet.server.user.domain.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BadgeCheckServiceImpl implements BadgeCheckService {

    private final BadgeRepository badgeRepository;
    private final BadgeCheckRepository badgeCheckRepository;
    private final BadgeCheckMapper badgeCheckMapper;
    private final UserRepository userRepository;

    @Override
    public BadgeCheckDto assignBadge(Long userId, BadgeCheckCreateRequest request) {
        log.info("[BadgeCheckService] 뱃지 부여 요청: userId={}, badgeId={}", userId, request.badgeId());
        BadgeCheck badgeCheck = badgeCheckRepository.findByUserIdAndBadgeId(userId, request.badgeId())
                .orElseGet(() -> {
                    Badge badge = badgeRepository.findById(request.badgeId())
                            .orElseThrow(BadgeNotFoundException::new);
                    User user = userRepository.findById(userId)
                            .orElseThrow(UserNotFoundException::new);
                    return badgeCheckMapper.toEntity(user, badge);
                });

        BadgeCheck saved = badgeCheckRepository.save(badgeCheck);
        log.info("[BadgeCheckService] 뱃지 부여 완료: badgeCheckId={}, userId={}, badgeId={}",
                saved.getId(), userId, request.badgeId());
        return badgeCheckMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BadgeCheckDto> getUserBadges(Long userId) {
        log.info("[BadgeCheckService] 사용자 뱃지 조회 요청: userId={}", userId);
        return badgeCheckMapper.toDtos(badgeCheckRepository.findAllByUserId(userId));
    }

    @Override
    public void revokeBadge(Long badgeCheckId) {
        BadgeCheck badgeCheck = badgeCheckRepository.findById(badgeCheckId)
                .orElseThrow(BadgeCheckNotFoundException::new);
        badgeCheckRepository.delete(badgeCheck);
        log.info("[BadgeCheckService] 뱃지 회수 완료: badgeCheckId={}", badgeCheckId);
    }
}
