package com.fitpet.server.mission.application.service;

import com.fitpet.server.mission.application.dto.MissionCreateCommand;
import com.fitpet.server.mission.application.dto.MissionResult;
import com.fitpet.server.mission.application.dto.MissionUpdateCommand;
import java.util.List;

public interface MissionService {

    MissionResult createMission(MissionCreateCommand request);

    MissionResult getMission(Long missionId);

    List<MissionResult> getMissions();

    MissionResult updateMission(Long missionId, MissionUpdateCommand request);

    void deleteMission(Long missionId);
}
