package com.fitpet.server.badge.presentation.controller;

import com.fitpet.server.badge.application.service.BadgeCheckService;
import com.fitpet.server.badge.application.service.BadgeService;
import com.fitpet.server.badge.application.dto.BadgeCheckCreateCommand;
import com.fitpet.server.badge.application.dto.BadgeCheckResult;
import com.fitpet.server.badge.application.dto.BadgeCreateCommand;
import com.fitpet.server.badge.application.dto.BadgeResult;
import com.fitpet.server.badge.application.dto.BadgeUpdateCommand;
import com.fitpet.server.badge.presentation.dto.BadgeCheckCreateRequest;
import com.fitpet.server.badge.presentation.dto.BadgeCheckDto;
import com.fitpet.server.badge.presentation.dto.BadgeCreateRequest;
import com.fitpet.server.badge.presentation.dto.BadgeDto;
import com.fitpet.server.badge.presentation.dto.BadgeUpdateRequest;
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
@RequestMapping("/badges")
@RequiredArgsConstructor
public class BadgeController {

    private final BadgeService badgeService;
    private final BadgeCheckService badgeCheckService;

    @PostMapping
    public ResponseEntity<BadgeDto> createBadge(
            @Valid @RequestBody BadgeCreateRequest request) {
        BadgeResult created = badgeService.createBadge(toCommand(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{badgeId}")
                .buildAndExpand(created.badgeId())
                .toUri();
        return ResponseEntity.created(location).body(toBadgeDto(created));
    }

    @GetMapping
    public ResponseEntity<List<BadgeDto>> getBadges() {
        List<BadgeDto> badges = badgeService.getBadges().stream()
                .map(BadgeController::toBadgeDto)
                .toList();
        return ResponseEntity.ok(badges);
    }

    @GetMapping("/{badgeId}")
    public ResponseEntity<BadgeDto> getBadge(@PathVariable Long badgeId) {
        return ResponseEntity.ok(toBadgeDto(badgeService.getBadge(badgeId)));
    }

    @PatchMapping("/{badgeId}")
    public ResponseEntity<BadgeDto> updateBadge(@PathVariable Long badgeId,
                                                @Valid @RequestBody BadgeUpdateRequest request) {
        return ResponseEntity.ok(toBadgeDto(badgeService.updateBadge(badgeId, toCommand(request))));
    }

    @DeleteMapping("/{badgeId}")
    public ResponseEntity<Void> deleteBadge(@PathVariable Long badgeId) {
        badgeService.deleteBadge(badgeId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/assign")
    public ResponseEntity<BadgeCheckDto> assignBadge(
            @AuthUser Long userId,
            @Valid @RequestBody BadgeCheckCreateRequest request) {

        BadgeCheckResult response = badgeCheckService.assignBadge(userId, new BadgeCheckCreateCommand(request.badgeId()));
        return ResponseEntity.ok(toBadgeCheckDto(response));
    }

    @GetMapping("/users")
    public ResponseEntity<List<BadgeCheckDto>> getUserBadges(
            @AuthUser Long userId
    ) {
        List<BadgeCheckDto> checks = badgeCheckService.getUserBadges(userId).stream()
                .map(BadgeController::toBadgeCheckDto)
                .toList();
        return ResponseEntity.ok(checks);
    }

    @DeleteMapping("/checks/{badgeCheckId}")
    public ResponseEntity<Void> revokeBadge(@PathVariable Long badgeCheckId) {
        badgeCheckService.revokeBadge(badgeCheckId);
        return ResponseEntity.noContent().build();
    }

    private static BadgeCreateCommand toCommand(BadgeCreateRequest request) {
        return new BadgeCreateCommand(
                request.title(),
                request.type(),
                request.conditionDuration(),
                request.conditionGoal(),
                request.description(),
                request.missionId()
        );
    }

    private static BadgeUpdateCommand toCommand(BadgeUpdateRequest request) {
        return new BadgeUpdateCommand(
                request.title(),
                request.type(),
                request.conditionDuration(),
                request.conditionGoal(),
                request.description(),
                request.missionId()
        );
    }

    private static BadgeDto toBadgeDto(BadgeResult result) {
        return new BadgeDto(
                result.badgeId(),
                result.title(),
                result.type(),
                result.conditionDuration(),
                result.conditionGoal(),
                result.description(),
                result.createdAt(),
                result.updatedAt()
        );
    }

    private static BadgeCheckDto toBadgeCheckDto(BadgeCheckResult result) {
        return new BadgeCheckDto(
                result.badgeCheckId(),
                result.userId(),
                result.badgeId(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}
