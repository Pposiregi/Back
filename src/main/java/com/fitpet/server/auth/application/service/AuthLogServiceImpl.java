package com.fitpet.server.auth.application.service;

import com.fitpet.server.auth.application.dto.CreateAuthLogCommand;
import com.fitpet.server.auth.domain.entity.AuthLog;
import com.fitpet.server.auth.domain.repository.AuthLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthLogServiceImpl implements AuthLogService {

    private final AuthLogRepository authLogRepository;

    @Override
    @Async
    public void record(CreateAuthLogCommand cmd) {
        try {
            authLogRepository.save(buildLog(cmd));
        } catch (Exception e) {
            log.error("[AuthLog] 로그 저장 실패 : userId={}, event={}", cmd.userId(), cmd.eventType(), e);
        }
    }

    private AuthLog buildLog(CreateAuthLogCommand cmd) {
        return AuthLog.builder()
                .userId(cmd.userId())
                .attemptedEmail(cmd.attemptedEmail())
                .eventType(cmd.eventType())
                .provider(cmd.provider())
                .ipAddress(cmd.ipAddress())
                .userAgent(cmd.userAgent())
                .success(cmd.success())
                .build();
    }
}
