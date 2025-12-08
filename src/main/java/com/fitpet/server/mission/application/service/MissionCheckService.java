package com.fitpet.server.mission.application.service;

import com.fitpet.server.mission.presentation.dto.MissionCheckDto;
import com.fitpet.server.mission.presentation.dto.MissionCheckRequest;
import java.util.List;

public interface MissionCheckService {

    MissionCheckDto upsertMissionCheck(Long missionId, Long userId, MissionCheckRequest request);

    List<MissionCheckDto> getMissionChecks(Long userId);

    void deleteMissionCheck(Long missionCheckId);
}
