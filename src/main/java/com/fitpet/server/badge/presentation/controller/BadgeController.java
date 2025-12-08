package com.fitpet.server.badge.presentation.controller;

import com.fitpet.server.badge.application.service.BadgeCheckService;
import com.fitpet.server.badge.application.service.BadgeService;
import com.fitpet.server.badge.presentation.dto.BadgeCheckCreateRequest;
import com.fitpet.server.badge.presentation.dto.BadgeCheckDto;
import com.fitpet.server.badge.presentation.dto.BadgeCreateRequest;
import com.fitpet.server.badge.presentation.dto.BadgeDto;
import com.fitpet.server.badge.presentation.dto.BadgeUpdateRequest;
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
        BadgeDto created = badgeService.createBadge(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{badgeId}")
                .buildAndExpand(created.badgeId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public ResponseEntity<List<BadgeDto>> getBadges() {
        return ResponseEntity.ok(badgeService.getBadges());
    }

    @GetMapping("/{badgeId}")
    public ResponseEntity<BadgeDto> getBadge(@PathVariable Long badgeId) {
        return ResponseEntity.ok(badgeService.getBadge(badgeId));
    }

    @PatchMapping("/{badgeId}")
    public ResponseEntity<BadgeDto> updateBadge(@PathVariable Long badgeId,
                                                @Valid @RequestBody BadgeUpdateRequest request) {
        return ResponseEntity.ok(badgeService.updateBadge(badgeId, request));
    }

    @DeleteMapping("/{badgeId}")
    public ResponseEntity<Void> deleteBadge(@PathVariable Long badgeId) {
        badgeService.deleteBadge(badgeId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/users/{userId}/assign")
    public ResponseEntity<BadgeCheckDto> assignBadge(@PathVariable Long userId,
                                                     @Valid @RequestBody BadgeCheckCreateRequest request) {
        BadgeCheckDto response = badgeCheckService.assignBadge(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<List<BadgeCheckDto>> getUserBadges(@PathVariable Long userId) {
        return ResponseEntity.ok(badgeCheckService.getUserBadges(userId));
    }

    @DeleteMapping("/checks/{badgeCheckId}")
    public ResponseEntity<Void> revokeBadge(@PathVariable Long badgeCheckId) {
        badgeCheckService.revokeBadge(badgeCheckId);
        return ResponseEntity.noContent().build();
    }
}
