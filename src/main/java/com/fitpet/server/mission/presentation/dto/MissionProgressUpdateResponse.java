package com.fitpet.server.mission.presentation.dto;

import java.util.List;

public record MissionProgressUpdateResponse(
        List<MissionProgressUpdateItem> updatedMissions
) {
}