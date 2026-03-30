package com.fitpet.server.auth.infra;

import com.fitpet.server.auth.domain.entity.AuthLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthLogJpaRepository extends JpaRepository<AuthLog, Long> {
}
