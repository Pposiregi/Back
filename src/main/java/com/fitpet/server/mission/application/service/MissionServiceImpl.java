package com.fitpet.server.mission.application.service;

import com.fitpet.server.mission.application.mapper.MissionMapper;
import com.fitpet.server.mission.domain.entity.Mission;
import com.fitpet.server.mission.domain.exception.MissionNotFoundException;
import com.fitpet.server.mission.domain.repository.MissionRepository;
import com.fitpet.server.mission.application.dto.MissionCreateCommand;
import com.fitpet.server.mission.application.dto.MissionResult;
import com.fitpet.server.mission.application.dto.MissionUpdateCommand;
import com.fitpet.server.mission.domain.entity.MissionCategory;
import com.fitpet.server.mission.domain.entity.MealMissionPolicy;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MissionServiceImpl implements MissionService {

    private final MissionRepository missionRepository;
    private final MissionMapper missionMapper;

    @Override
    public MissionResult createMission(MissionCreateCommand request) {
        log.info("[MissionService] 미션 생성 요청: title={}, type={}, category={}, goal={}",
            request.title(), request.type(), request.category(), request.goal());
        MealMissionPolicy mealPolicy = resolveMealPolicyForCreate(
                request.category(),
                request.mealPolicy(),
                request.title()
        );
        Mission mission = missionMapper.toEntity(request);
        applyMealPolicy(mission, request.category(), mealPolicy);
        Mission saved = missionRepository.save(mission);
        log.info("[MissionService] 미션 생성: missionId={}", saved.getId());
        return missionMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MissionResult> getMissions() {
        log.info("[MissionService] 미션 전체 조회 요청");
        return missionMapper.toDtos(missionRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public MissionResult getMission(Long missionId) {
        log.info("[MissionService] 미션 단건 조회 요청: missionId={}", missionId);
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(MissionNotFoundException::new);
        log.info("[MissionService] 미션 단건 조회 완료: missionId={}", missionId);
        return missionMapper.toDto(mission);
    }

    @Override
    public MissionResult updateMission(Long missionId, MissionUpdateCommand request) {
        log.info("[MissionService] 미션 수정 요청: missionId={}, title={}, type={}, category={}, goal={}",
            missionId, request.title(), request.type(), request.category(), request.goal());
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(MissionNotFoundException::new);
        MissionCategory targetCategory = request.category() != null ? request.category() : mission.getCategory();
        mission.update(
                request.title(),
                request.content(),
                request.description(),
                request.type(),
                request.category(),
                resolveMealPolicyForUpdate(mission, targetCategory, request.mealPolicy(), request.title()),
                request.goal()
        );
        Mission updated = missionRepository.save(mission);
        log.info("[MissionService] 미션 수정: missionId={}", updated.getId());
        return missionMapper.toDto(updated);
    }

    @Override
    public void deleteMission(Long missionId) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(MissionNotFoundException::new);
        missionRepository.delete(mission);
        log.info("[MissionService] 미션 삭제: missionId={}", missionId);
    }

    private static MealMissionPolicy resolveMealPolicyForCreate(
            MissionCategory category,
            MealMissionPolicy mealPolicy,
            String title
    ) {
        if (category != MissionCategory.MEAL) {
            return null;
        }
        return mealPolicy != null ? mealPolicy : MealMissionPolicy.infer(title);
    }

    private static MealMissionPolicy resolveMealPolicyForUpdate(
            Mission mission,
            MissionCategory targetCategory,
            MealMissionPolicy mealPolicy,
            String title
    ) {
        if (targetCategory != MissionCategory.MEAL) {
            return null;
        }
        if (mealPolicy != null) {
            return mealPolicy;
        }
        if (mission.getMealPolicy() != null) {
            return mission.getMealPolicy();
        }
        String sourceTitle = title != null ? title : mission.getTitle();
        return MealMissionPolicy.infer(sourceTitle);
    }

    private static void applyMealPolicy(Mission mission, MissionCategory category, MealMissionPolicy mealPolicy) {
        mission.update(
                null,
                null,
                null,
                null,
                category,
                mealPolicy,
                null
        );
    }
}
