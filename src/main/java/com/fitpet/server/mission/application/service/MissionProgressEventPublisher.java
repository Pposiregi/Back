package com.fitpet.server.mission.application.service;

import com.fitpet.server.mission.application.dto.MissionProgressEvent;

public interface MissionProgressEventPublisher {

    void publishAfterCommit(MissionProgressEvent event);
}
