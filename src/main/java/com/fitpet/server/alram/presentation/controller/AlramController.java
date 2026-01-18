package com.fitpet.server.alram.presentation.controller;

import com.fitpet.server.alram.application.service.AlramService;
import com.fitpet.server.alram.presentation.dto.request.AlramRequestDto;
import com.fitpet.server.alram.presentation.dto.response.AlramResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/alram")
@RequiredArgsConstructor
public class AlramController {

    private final AlramService alramService;

    @PostMapping("/send")
    public ResponseEntity<AlramResponseDto> sendAlram(
            @Valid @RequestBody AlramRequestDto requestDto
    ) {
        AlramResponseDto response = alramService.sendAndSaveAlram(requestDto);
        log.info("✅FCM 알림 컨트롤러");
        return ResponseEntity.ok(response);
    }
}