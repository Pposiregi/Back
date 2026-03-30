package com.fitpet.server.mission.application.dto;

import com.fitpet.server.mission.domain.entity.MissionCheck;

public record MissionCompletionResult(
        MissionCheck missionCheck,
        Integer clearCount
) {
}
