package com.fitpet.server.auth.domain.repository;

import com.fitpet.server.auth.domain.entity.AuthLog;

public interface AuthLogRepository {
    void save(AuthLog authLog);
}
