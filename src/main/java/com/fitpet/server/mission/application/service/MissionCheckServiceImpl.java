package com.fitpet.server.mission.application.service;

import com.fitpet.server.mission.application.mapper.MissionCheckMapper;
import com.fitpet.server.mission.domain.entity.Mission;
import com.fitpet.server.mission.domain.entity.MissionCheck;
import com.fitpet.server.mission.domain.exception.MissionCheckNotFoundException;
import com.fitpet.server.mission.domain.exception.MissionNotFoundException;
import com.fitpet.server.mission.domain.repository.MissionCheckRepository;
import com.fitpet.server.mission.domain.repository.MissionRepository;
import com.fitpet.server.mission.presentation.dto.MissionCheckDto;
import com.fitpet.server.mission.presentation.dto.MissionCheckRequest;
import com.fitpet.server.pet.application.service.PetExpressionService;
import com.fitpet.server.pet.domain.entity.PetExpression;
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
public class MissionCheckServiceImpl implements MissionCheckService {

    private final MissionRepository missionRepository;
    private final MissionCheckRepository missionCheckRepository;
    private final MissionCheckMapper missionCheckMapper;
    private final UserRepository userRepository;
    private final PetExpressionService petExpressionService;

    @Override
    public MissionCheckDto upsertMissionCheck(Long missionId, Long userId, MissionCheckRequest request) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(MissionNotFoundException::new);
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        final boolean[] shouldCelebrate = {false};

        MissionCheck missionCheck = missionCheckRepository
                .findByMissionIdAndUserIdAndCheckAt(missionId, userId, request.checkDate())
                .map(existing -> {
                    boolean wasCompleted = existing.isCompleted();
                    missionCheckMapper.updateFromRequest(existing, request);
                    if (!wasCompleted && Boolean.TRUE.equals(request.completed())) {
                        shouldCelebrate[0] = true;
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    if (Boolean.TRUE.equals(request.completed())) {
                        shouldCelebrate[0] = true;
                    }
                    return missionCheckMapper.create(mission, user, request);
                });

        MissionCheck saved = missionCheckRepository.save(missionCheck);

        if (shouldCelebrate[0]) {
            petExpressionService.updateExpression(userId, PetExpression.HAPPY);
        }

        log.info("[MissionCheckService] 수행 여부 저장: missionCheckId={}, missionId={}, userId={}",
                saved.getId(), missionId, userId);
        return missionCheckMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MissionCheckDto> getMissionChecks(Long userId) {
        List<MissionCheck> checks = missionCheckRepository.findAllByUserId(userId);
        return missionCheckMapper.toDtos(checks);
    }

    @Override
    public void deleteMissionCheck(Long missionCheckId) {
        MissionCheck missionCheck = missionCheckRepository.findById(missionCheckId)
                .orElseThrow(MissionCheckNotFoundException::new);
        missionCheckRepository.delete(missionCheck);
        log.info("[MissionCheckService] 수행 여부 삭제: missionCheckId={}", missionCheckId);
    }
}
