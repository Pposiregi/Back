package com.fitpet.server.mission.presentation.dto;

import java.util.List;

public record MissionProgressListResponse(
        List<MissionProgressResponse> missions

) {
}
