package com.fitpet.server.mission.infra.jpa;

import com.fitpet.server.mission.domain.entity.Mission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MissionJpaRepository extends JpaRepository<Mission, Long> {
}
