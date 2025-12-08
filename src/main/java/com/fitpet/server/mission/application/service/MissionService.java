package com.fitpet.server.mission.application.service;

import com.fitpet.server.mission.presentation.dto.MissionCreateRequest;
import com.fitpet.server.mission.presentation.dto.MissionDto;
import com.fitpet.server.mission.presentation.dto.MissionUpdateRequest;
import java.util.List;

public interface MissionService {

    MissionDto createMission(MissionCreateRequest request);

    MissionDto getMission(Long missionId);

    List<MissionDto> getMissions();

    MissionDto updateMission(Long missionId, MissionUpdateRequest request);

    void deleteMission(Long missionId);
}
