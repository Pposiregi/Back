package com.fitpet.server.badge.application.service;

import com.fitpet.server.badge.application.mapper.BadgeMapper;
import com.fitpet.server.badge.domain.entity.Badge;
import com.fitpet.server.badge.domain.exception.BadgeNotFoundException;
import com.fitpet.server.badge.domain.repository.BadgeRepository;
import com.fitpet.server.badge.presentation.dto.BadgeCreateRequest;
import com.fitpet.server.badge.presentation.dto.BadgeDto;
import com.fitpet.server.badge.presentation.dto.BadgeUpdateRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BadgeServiceImpl implements BadgeService {

    private final BadgeRepository badgeRepository;
    private final BadgeMapper badgeMapper;

    @Override
    public BadgeDto createBadge(BadgeCreateRequest request) {
        log.info("[BadgeService] 뱃지 생성 요청: title={}, type={}", request.title(), request.type());
        Badge badge = badgeMapper.toEntity(request);
        Badge saved = badgeRepository.save(badge);
        log.info("[BadgeService] 뱃지 생성 완료: id={}", saved.getId());
        return badgeMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public BadgeDto getBadge(Long badgeId) {
        log.info("[BadgeService] 뱃지 조회 요청: id={}", badgeId);
        Badge badge = badgeRepository.findById(badgeId)
                .orElseThrow(BadgeNotFoundException::new);
        return badgeMapper.toDto(badge);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BadgeDto> getBadges() {
        log.info("[BadgeService] 뱃지 전체 조회 요청");
        return badgeMapper.toDtos(badgeRepository.findAll());
    }

    @Override
    public BadgeDto updateBadge(Long badgeId, BadgeUpdateRequest request) {
        log.info("[BadgeService] 뱃지 수정 요청: id={}", badgeId);
        Badge badge = badgeRepository.findById(badgeId)
                .orElseThrow(BadgeNotFoundException::new);
        badge.update(request.title(), request.type(), request.conditionDuration(), request.conditionGoal(),
                request.description());
        return badgeMapper.toDto(badge);
    }

    @Override
    public void deleteBadge(Long badgeId) {
        Badge badge = badgeRepository.findById(badgeId)
                .orElseThrow(BadgeNotFoundException::new);
        badgeRepository.delete(badge);
        log.info("[BadgeService] 뱃지 삭제 완료: id={}", badgeId);
    }
}
