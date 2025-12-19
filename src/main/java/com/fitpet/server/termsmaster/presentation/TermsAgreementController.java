package com.fitpet.server.termsmaster.presentation;

import com.fitpet.server.shared.annotation.AuthUser;
import com.fitpet.server.termsmaster.application.dto.TermsAgreementCommand;
import com.fitpet.server.termsmaster.application.service.TermsAgreementService;
import com.fitpet.server.termsmaster.presentation.dto.TermsAgreementRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/terms/agreements")
@RequiredArgsConstructor
public class TermsAgreementController {

    private final TermsAgreementService termsAgreementService;

    @PostMapping
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