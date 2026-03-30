package com.fitpet.server.auth.infra;

import com.fitpet.server.auth.domain.entity.AuthLog;
import com.fitpet.server.auth.domain.repository.AuthLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AuthLogRepositoryAdapter implements AuthLogRepository {

    private final AuthLogJpaRepository jpaRepository;

    @Override
    public void save(AuthLog authLog) {
        jpaRepository.save(authLog);
    }
}
