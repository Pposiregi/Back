package com.fitpet.server.termsmaster.presentation;

import com.fitpet.server.termsmaster.application.dto.TermsDto;
import com.fitpet.server.termsmaster.application.service.TermsService;
import com.fitpet.server.termsmaster.presentation.dto.TermsResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/terms")
@RequiredArgsConstructor
public class TermsController {

    private final TermsService termsService;

    @GetMapping
    public ResponseEntity<List<TermsResponse>> getTerms() {
        List<TermsDto> termsDtos = termsService.getActiveTerms();

        List<TermsResponse> response = termsDtos.stream()
                .map(TermsResponse::from)
                .toList();

        return ResponseEntity.ok(response);
    }
}