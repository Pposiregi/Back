package com.fitpet.server.mission.application.service;

import com.fitpet.server.mission.presentation.dto.MissionCheckDto;
import com.fitpet.server.mission.presentation.dto.MissionCheckRequest;
import com.fitpet.server.mission.presentation.dto.MissionProgressResponse;
import com.fitpet.server.mission.presentation.dto.MissionProgressUpdateItem;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface MissionCheckService {

    MissionCheckDto upsertMissionCheck(Long missionId, Long userId, MissionCheckRequest request);

    List<MissionCheckDto> getMissionChecks(Long userId);

    void deleteMissionCheck(Long userId, Long missionCheckId);

    List<MissionProgressResponse> getActiveMissions(Long userId, LocalDate date);

    List<MissionProgressResponse> getCompletedMissions(Long userId);

    List<MissionProgressUpdateItem> updateMealMissions(Long userId, LocalDate actionDate);

    List<MissionProgressUpdateItem> updateStepMissions(
        Long userId,
        LocalDate actionDate,
        BigDecimal increment
    );
}
