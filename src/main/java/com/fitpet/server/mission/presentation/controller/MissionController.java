package com.fitpet.server.mission.presentation.controller;

import com.fitpet.server.mission.application.service.MissionCheckService;
import com.fitpet.server.mission.application.service.MissionService;
import com.fitpet.server.mission.presentation.dto.MissionCheckDto;
import com.fitpet.server.mission.presentation.dto.MissionCheckRequest;
import com.fitpet.server.mission.presentation.dto.MissionCreateRequest;
import com.fitpet.server.mission.presentation.dto.MissionDto;
import com.fitpet.server.mission.presentation.dto.MissionUpdateRequest;
import com.fitpet.server.shared.annotation.AuthUser;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Slf4j
@RestController
@RequestMapping("/missions")
@RequiredArgsConstructor
public class MissionController {

    private final MissionService missionService;
    private final MissionCheckService missionCheckService;

    @PostMapping
    public ResponseEntity<MissionDto> createMission(@Valid @RequestBody MissionCreateRequest request) {
        log.info("[MissionController] 미션 생성 요청: title={}, type={}", request.title(), request.type());
        MissionDto created = missionService.createMission(request);
        log.info("[MissionController] 미션 생성 완료: missionId={}, title={}, type={}", created.missionId(),
                created.title(), created.type());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{missionId}")
                .buildAndExpand(created.missionId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public ResponseEntity<List<MissionDto>> getMissions() {
        log.info("[MissionController] 미션 전체 조회 요청");
        List<MissionDto> responses = missionService.getMissions();
        log.info("[MissionController] 미션 전체 조회 완료: count={}", responses.size());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{missionId}")
    public ResponseEntity<MissionDto> getMission(@PathVariable Long missionId) {
        log.info("[MissionController] 미션 단건 조회 요청: missionId={}", missionId);
        MissionDto response = missionService.getMission(missionId);
        log.info("[MissionController] 미션 단건 조회 완료: missionId={}", missionId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{missionId}")
    public ResponseEntity<MissionDto> updateMission(@PathVariable Long missionId,
                                                    @Valid @RequestBody MissionUpdateRequest request) {
        log.info("[MissionController] 미션 수정 요청: missionId={}, title={}, type={}", missionId, request.title(),
                request.type());
        MissionDto updated = missionService.updateMission(missionId, request);
        log.info("[MissionController] 미션 수정 완료: missionId={}", missionId);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{missionId}")
    public ResponseEntity<Void> deleteMission(@PathVariable Long missionId) {
        missionService.deleteMission(missionId);
        log.info("[MissionController] 미션 삭제 완료: missionId={}", missionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{missionId}/checks")
    public ResponseEntity<MissionCheckDto> upsertMissionCheck(
            @PathVariable Long missionId,
            @AuthUser Long userId, // URL 대신 토큰에서 추출
            @Valid @RequestBody MissionCheckRequest request) {

        log.info("[MissionController] 미션 수행 여부 저장 요청: missionId={}, userId={}, date={}, completed={}",
                missionId, userId, request.checkDate(), request.completed());

        MissionCheckDto response = missionCheckService.upsertMissionCheck(missionId, userId, request);

        log.info("[MissionController] 미션 수행 여부 저장 완료: missionCheckId={}, missionId={}, userId={}",
                response.missionCheckId(), missionId, userId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/checks")
    public ResponseEntity<List<MissionCheckDto>> getMissionChecks(@AuthUser Long userId) {
        log.info("[MissionController] 사용자 수행 기록 조회 요청: userId={}", userId);

        List<MissionCheckDto> responses = missionCheckService.getMissionChecks(userId);

        log.info("[MissionController] 사용자 수행 기록 조회 완료: userId={}, count={}", userId, responses.size());

        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/checks/{missionCheckId}")
    public ResponseEntity<Void> deleteMissionCheck(
            @AuthUser Long userId,
            @PathVariable Long missionCheckId
    ) {
        missionCheckService.deleteMissionCheck(userId, missionCheckId);

        log.info("[MissionController] 미션 수행 기록 삭제 완료: missionCheckId={}", missionCheckId);

        return ResponseEntity.noContent().build();
    }
}