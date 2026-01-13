package com.fitpet.server.mission.application.service;

import com.fitpet.server.mission.application.dto.MissionCheckCommand;
import com.fitpet.server.mission.application.dto.MissionCheckResult;
import com.fitpet.server.mission.application.dto.MissionProgressResult;
import com.fitpet.server.mission.application.dto.MissionProgressUpdateItem;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface MissionCheckService {

    MissionCheckResult upsertMissionCheck(Long missionId, Long userId, MissionCheckCommand request);

    List<MissionCheckResult> getMissionChecks(Long userId);

    void deleteMissionCheck(Long userId, Long missionCheckId);

    MissionCheckResult completeMissionCheck(Long userId, Long missionCheckId);

    List<MissionProgressResult> getActiveMissions(Long userId, LocalDate date);

    List<MissionProgressResult> getCompletedMissions(Long userId);

    List<MissionProgressUpdateItem> updateMealMissions(Long userId, LocalDate actionDate);

    List<MissionProgressUpdateItem> updateStepMissions(
        Long userId,
        LocalDate actionDate,
        BigDecimal increment
    );
}
