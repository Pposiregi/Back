package com.fitpet.server.auth.application.service;

import com.fitpet.server.auth.application.dto.CreateAuthLogCommand;

public interface AuthLogService {
    void record(CreateAuthLogCommand command);
}
