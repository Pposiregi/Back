package com.fitpet.server.termsmaster.presentation;

import com.fitpet.server.shared.annotation.AuthUser;
import com.fitpet.server.termsmaster.application.dto.TermsAgreementCommand;
import com.fitpet.server.termsmaster.application.dto.TermsDto;
import com.fitpet.server.termsmaster.application.service.TermsAgreementService;
import com.fitpet.server.termsmaster.application.service.TermsService;
import com.fitpet.server.termsmaster.presentation.dto.TermsAgreementRequest;
import com.fitpet.server.termsmaster.presentation.dto.TermsResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/terms")
@RequiredArgsConstructor
public class TermsController {

    private final TermsService termsService;
    private final TermsAgreementService termsAgreementService;

    @GetMapping
    public ResponseEntity<List<TermsResponse>> getTerms() {
        List<TermsDto> termsDtos = termsService.getActiveTerms();

        List<TermsResponse> response = termsDtos.stream()
                .map(TermsResponse::from)
                .toList();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/agreements")
    public ResponseEntity<String> saveAgreements(
            @AuthUser Long userId,
            @RequestBody @Valid List<TermsAgreementRequest> requests
    ) {
        List<TermsAgreementCommand> commands = requests.stream()
                .map(TermsAgreementRequest::toCommand)
                .toList();

        termsAgreementService.saveTermsAgreements(userId, commands);

        return ResponseEntity.ok("약관 동의 내역이 저장되었습니다.");
    }
}