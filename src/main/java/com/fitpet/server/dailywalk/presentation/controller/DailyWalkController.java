package com.fitpet.server.dailywalk.presentation.controller;

import com.fitpet.server.dailywalk.application.service.DailyWalkService;
import com.fitpet.server.dailywalk.presentation.dto.request.DailyWalkCreateRequest;
import com.fitpet.server.dailywalk.presentation.dto.request.DailyWalkStepUpdateRequest;
import com.fitpet.server.dailywalk.presentation.dto.response.DailyStepSummaryResponse;
import com.fitpet.server.dailywalk.presentation.dto.response.DailyWalkResponse;
import com.fitpet.server.shared.annotation.AuthUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PastOrPresent;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/daily/walks")
@RequiredArgsConstructor
@Validated
public class DailyWalkController {

    private final DailyWalkService dailyWalkService;

    @GetMapping
    public ResponseEntity<List<DailyWalkResponse>> listByUser(@AuthUser Long userId) {
        List<DailyWalkResponse> body = dailyWalkService.getAllByUserId(userId);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/steps/weekly")
    public ResponseEntity<List<DailyStepSummaryResponse>> getWeeklySteps(@AuthUser Long userId) {
        List<DailyStepSummaryResponse> body = dailyWalkService.getWeeklySteps(userId);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/date")
    public ResponseEntity<DailyWalkResponse> getByUserAndDate(
            @AuthUser Long userId,
            @RequestParam("date")
            @PastOrPresent
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        DailyWalkResponse dailyWalk = dailyWalkService.getDailyWalkByUserIdAndDate(userId, date);
        return ResponseEntity.ok(dailyWalk);
    }

    @PostMapping
    public ResponseEntity<DailyWalkResponse> create(
            @AuthUser Long userId,
            @RequestBody @Valid DailyWalkCreateRequest req
    ) {
        DailyWalkResponse saved = dailyWalkService.createDailyWalk(userId, req);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.id())
                .toUri();

        return ResponseEntity.created(location).body(saved);
    }

    @PatchMapping("/step")
    public ResponseEntity<Void> updateStepByUserAndDate(
            @AuthUser Long userId,
            @RequestBody @Valid DailyWalkStepUpdateRequest req
    ) {
        dailyWalkService.updateDailyWalkStep(userId, req);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{dailyWalkId}")
    public ResponseEntity<Void> delete(
            @AuthUser Long userId,
            @PathVariable Long dailyWalkId
    ) {
        dailyWalkService.deleteDailyWalk(userId, dailyWalkId);
        return ResponseEntity.noContent().build();
    }
}