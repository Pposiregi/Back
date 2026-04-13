package com.fitpet.server.termsmaster.presentation;

import com.fitpet.server.shared.annotation.AuthUser;
import com.fitpet.server.termsmaster.application.service.TermsAgreementService;
import com.fitpet.server.termsmaster.application.service.TermsService;
import com.fitpet.server.termsmaster.presentation.dto.TermsAgreementRequest;
import com.fitpet.server.termsmaster.presentation.dto.TermsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Terms", description = "약관 조회 및 동의 API")
public class TermsController {

    private final TermsService termsService;
    private final TermsAgreementService termsAgreementService;

    @Operation(
            summary = "약관 목록 조회",
            description = "version 파라미터가 없으면 현재 유효한 최신 약관을 반환합니다. version을 지정하면 해당 버전의 약관을 반환합니다."
    )
    @GetMapping
    public ResponseEntity<List<TermsResponse>> getTerms(
            @Parameter(description = "조회할 약관 버전 (예: 1.0, 2.0). 미입력 시 최신 약관 반환")
            @RequestParam(required = false) String version
    ) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(termsService.getTerms(version).stream().map(TermsResponse::from).toList());
    }

    @Operation(
            summary = "약관 동의 저장",
            description = "사용자의 약관 동의 여부를 저장합니다. 이미 동의한 약관은 상태가 업데이트됩니다. " +
                    "필수 약관(SERVICE_USE, PRIVACY_COLLECTION, PRIVACY_POLICY, LOCATION_BASED, HEALTH_INFO)은 모두 포함되어야 하며 isAgreed=true여야 합니다."
    )
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
