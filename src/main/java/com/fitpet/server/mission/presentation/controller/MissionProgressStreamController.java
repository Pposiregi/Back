package com.fitpet.server.mission.presentation.controller;

import com.fitpet.server.mission.application.service.MissionProgressStreamService;
import com.fitpet.server.shared.annotation.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/missions/progress")
public class MissionProgressStreamController {

    private final MissionProgressStreamService missionProgressStreamService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthUser Long userId) {
        return missionProgressStreamService.subscribe(userId);
    }
}
