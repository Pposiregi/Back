package com.fitpet.server.termsmaster.presentation;

import com.fitpet.server.shared.annotation.AuthUser;
import com.fitpet.server.termsmaster.application.service.TermsAgreementService;
import com.fitpet.server.termsmaster.application.service.TermsService;
import com.fitpet.server.termsmaster.presentation.dto.TermsAgreementRequest;
import com.fitpet.server.termsmaster.presentation.dto.TermsResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/terms")
public class TermsController {

    private final TermsService termsService;
    private final TermsAgreementService termsAgreementService;

    @GetMapping
    public ResponseEntity<List<TermsResponse>> getTerms(
            @RequestParam(required = false) String version
    ) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(termsService.getTerms(version).stream().map(TermsResponse::from).toList());
    }

    @PostMapping("/agreements")
    public ResponseEntity<Void> saveAgreements(
            @AuthUser Long userId,
            @Valid @RequestBody List<TermsAgreementRequest> requests
    ) {
        termsAgreementService.saveTermsAgreements(userId, TermsAgreementRequest.toCommands(requests));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    //TODO : 우선 MDL로 관리 후 추후 관리자만 요청할 수 있게 수정
//    @PostMapping("/modify")
//    public ResponseEntity<Void> createTerms(@RequestBody TermsCreateRequest request) {
//        termsService.createTerms(request.code(), request.content(), request.version());
//        return ResponseEntity.status(HttpStatus.CREATED).build();
//    }
}
