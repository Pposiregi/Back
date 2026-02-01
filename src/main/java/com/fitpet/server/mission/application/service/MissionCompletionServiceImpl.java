package com.fitpet.server.mission.application.service;

import com.fitpet.server.badge.domain.entity.Badge;
import com.fitpet.server.badge.domain.entity.BadgeCheck;
import com.fitpet.server.badge.domain.repository.BadgeCheckRepository;
import com.fitpet.server.badge.domain.repository.BadgeRepository;
import com.fitpet.server.mission.domain.entity.Mission;
import com.fitpet.server.mission.domain.entity.MissionCheck;
import com.fitpet.server.mission.domain.entity.UserMissionStat;
import com.fitpet.server.mission.domain.repository.MissionCheckRepository;
import com.fitpet.server.mission.domain.repository.UserMissionStatRepository;
import com.fitpet.server.shared.exception.BusinessException;
import com.fitpet.server.shared.exception.ErrorCode;
import com.fitpet.server.user.domain.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MissionCompletionServiceImpl implements MissionCompletionService {

    private final MissionCheckRepository missionCheckRepository;
    private final UserMissionStatRepository userMissionStatRepository;
    private final BadgeRepository badgeRepository;
    private final BadgeCheckRepository badgeCheckRepository;

    @Transactional
    @Override
    public MissionCheck completeMission(Long userId, Long missionCheckId) {
        MissionCheck missionCheck = missionCheckRepository.findByIdForUpdate(missionCheckId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MISSION_CHECK_NOT_FOUND));

        if (!missionCheck.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.MISSION_CHECK_ACCESS_DENIED);
        }

        if (missionCheck.isCompleted()) {
            return missionCheck;
        }

        if (!isGoalReached(missionCheck)) {
            throw new BusinessException(ErrorCode.MISSION_CHECK_NOT_COMPLETABLE);
        }

        missionCheck.updateProgress(missionCheck.getProgressValue(), true, LocalDateTime.now());
        missionCheckRepository.save(missionCheck);

        UserMissionStat stat = adjustClearCount(missionCheck);
        awardBadges(missionCheck.getUser(), missionCheck.getMission(), stat.getClearCount());
        return missionCheck;
    }

    private boolean isGoalReached(MissionCheck missionCheck) {
        if (missionCheck.getProgressValue() == null || missionCheck.getMission().getGoal() == null) {
            return false;
        }
        return missionCheck.getProgressValue().compareTo(missionCheck.getMission().getGoal()) >= 0;
    }

    private UserMissionStat adjustClearCount(MissionCheck missionCheck) {
        return userMissionStatRepository.findWithLockByUserAndMission(
                        missionCheck.getUser(),
                        missionCheck.getMission()
                )
                .map(stat -> {
                    stat.incrementClearCount();
                    return stat;
                })
                .orElseGet(() -> createStat(missionCheck));
    }

    private UserMissionStat createStat(MissionCheck missionCheck) {
        try {
            UserMissionStat stat = UserMissionStat.builder()
                    .user(missionCheck.getUser())
                    .mission(missionCheck.getMission())
                    .clearCount(1)
                    .build();
            return userMissionStatRepository.save(stat);
        } catch (DataIntegrityViolationException ex) {
            UserMissionStat stat = userMissionStatRepository.findWithLockByUserAndMission(
                            missionCheck.getUser(),
                            missionCheck.getMission()
                    )
                    .orElseThrow(() -> new BusinessException(ErrorCode.MISSION_STAT_DUPLICATE));
            stat.incrementClearCount();
            return stat;
        }
    }

    private void awardBadges(User user, Mission mission, Integer clearCount) {
        if (clearCount == null) {
            return;
        }

        List<Badge> badges = badgeRepository.findEligibleByMissionId(
                mission.getId(),
                clearCount.longValue()
        );

        for (Badge badge : badges) {
            // TODO: conditionDuration 기반 "N일 이내 M회" 조건 확장 지점.
            if (badgeCheckRepository.existsByUserIdAndBadgeId(user.getId(), badge.getId())) {
                continue;
            }
            try {
                BadgeCheck badgeCheck = BadgeCheck.builder()
                        .user(user)
                        .badge(badge)
                        .build();
                badgeCheckRepository.save(badgeCheck);
            } catch (DataIntegrityViolationException ignored) {
                log.debug("이미 부여된 뱃지입니다. userId={} badgeId={}", user.getId(), badge.getId());
            }
        }
    }
}
