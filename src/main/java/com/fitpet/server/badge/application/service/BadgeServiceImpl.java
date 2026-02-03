package com.fitpet.server.badge.application.service;

import com.fitpet.server.badge.application.dto.BadgeCreateCommand;
import com.fitpet.server.badge.application.dto.BadgeResult;
import com.fitpet.server.badge.application.dto.BadgeUpdateCommand;
import com.fitpet.server.badge.application.mapper.BadgeMapper;
import com.fitpet.server.badge.domain.entity.Badge;
import com.fitpet.server.badge.domain.exception.BadgeNotFoundException;
import com.fitpet.server.badge.domain.repository.BadgeRepository;
import com.fitpet.server.mission.domain.entity.Mission;
import com.fitpet.server.mission.domain.exception.MissionNotFoundException;
import com.fitpet.server.mission.domain.repository.MissionRepository;
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
    private final MissionRepository missionRepository;

    // 관리자 전용
    @Override
    public BadgeResult createBadge(BadgeCreateCommand command) {
        log.info("[BadgeService] 뱃지 생성 요청: title={}, type={}", command.title(), command.type());
        Badge badge = badgeMapper.toEntity(command);
        Mission mission = missionRepository.findById(command.missionId())
                .orElseThrow(MissionNotFoundException::new);
        badge.assignMission(mission);
        Badge saved = badgeRepository.save(badge);
        log.info("[BadgeService] 뱃지 생성 완료: id={}", saved.getId());
        return badgeMapper.toResult(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public BadgeResult getBadge(Long badgeId) {
        log.info("[BadgeService] 뱃지 조회 요청: id={}", badgeId);
        Badge badge = badgeRepository.findById(badgeId)
                .orElseThrow(BadgeNotFoundException::new);
        return badgeMapper.toResult(badge);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BadgeResult> getBadges() {
        log.info("[BadgeService] 뱃지 전체 조회 요청");
        return badgeMapper.toResults(badgeRepository.findAll());
    }

    // 관리자 전용
    @Override
    public BadgeResult updateBadge(Long badgeId, BadgeUpdateCommand command) {
        log.info("[BadgeService] 뱃지 수정 요청: id={}", badgeId);
        Badge badge = badgeRepository.findById(badgeId)
                .orElseThrow(BadgeNotFoundException::new);
        Mission mission = null;
        if (command.missionId() != null) {
            mission = missionRepository.findById(command.missionId())
                    .orElseThrow(MissionNotFoundException::new);
        }
        badge.update(command.title(), command.type(), command.conditionDuration(), command.conditionGoal(),
                command.description(), mission);
        return badgeMapper.toResult(badge);
    }

    @Override
    public void deleteBadge(Long badgeId) {
        Badge badge = badgeRepository.findById(badgeId)
                .orElseThrow(BadgeNotFoundException::new);
        badgeRepository.delete(badge);
        log.info("[BadgeService] 뱃지 삭제 완료: id={}", badgeId);
    }
}
