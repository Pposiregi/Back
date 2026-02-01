package com.fitpet.server.mission.application.service;

import com.fitpet.server.mission.domain.entity.MissionCheck;

public interface MissionCompletionService {

    MissionCheck completeMission(Long userId, Long missionCheckId);
}
