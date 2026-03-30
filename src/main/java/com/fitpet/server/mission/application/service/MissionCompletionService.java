package com.fitpet.server.mission.application.service;

import com.fitpet.server.mission.application.dto.MissionCompletionResult;

public interface MissionCompletionService {

    MissionCompletionResult completeMission(Long userId, Long missionCheckId);
}
