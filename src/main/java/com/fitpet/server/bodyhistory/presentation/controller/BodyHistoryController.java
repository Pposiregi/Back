package com.fitpet.server.bodyhistory.presentation.controller;

import com.fitpet.server.bodyhistory.application.service.BodyHistoryService;
import com.fitpet.server.bodyhistory.presentation.dto.request.BodyHistoryCreateRequest;
import com.fitpet.server.bodyhistory.presentation.dto.request.BodyHistoryUpdateRequest;
import com.fitpet.server.bodyhistory.presentation.dto.response.BodyHistoryResponse;
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
@RequestMapping("/body-histories")
@RequiredArgsConstructor
@Validated
public class BodyHistoryController {

    private final BodyHistoryService bodyHistoryService;

    @GetMapping
    public ResponseEntity<List<BodyHistoryResponse>> listByUser(@AuthUser Long userId) {
        List<BodyHistoryResponse> body = bodyHistoryService.findAllBodyHistoriesByUserId(userId);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/date")
    public ResponseEntity<BodyHistoryResponse> getByUserAndDate(
            @AuthUser Long userId,
            @RequestParam("date") @PastOrPresent @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<BodyHistoryResponse> body = bodyHistoryService.findMonthlyBodyHistories(userId, date.getYear(),
                date.getMonthValue());

        if (body.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(body.get(0));
    }

    @PostMapping
    public ResponseEntity<BodyHistoryResponse> create(@RequestBody @Valid BodyHistoryCreateRequest request) {
        BodyHistoryResponse saved = bodyHistoryService.createBodyHistory(request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.id())
                .toUri();

        return ResponseEntity.created(location).body(saved);
    }

    @PatchMapping("/{historyId}")
    public ResponseEntity<BodyHistoryResponse> update(
            @PathVariable Long historyId,
            @RequestBody @Valid BodyHistoryUpdateRequest request) {
        BodyHistoryResponse updatedResponse = bodyHistoryService.updateBodyHistory(historyId, request);
        return ResponseEntity.ok(updatedResponse);
    }

    @DeleteMapping("/{historyId}")
    public ResponseEntity<Void> delete(@PathVariable Long historyId) {
        bodyHistoryService.deleteBodyHistory(historyId);
        return ResponseEntity.noContent().build();
    }
}