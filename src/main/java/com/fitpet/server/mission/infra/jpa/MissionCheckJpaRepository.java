package com.fitpet.server.mission.infra.jpa;

import com.fitpet.server.mission.domain.entity.MissionCheck;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MissionCheckJpaRepository extends JpaRepository<MissionCheck, Long> {

    Optional<MissionCheck> findByMissionIdAndUserIdAndCheckAt(Long missionId, Long userId, LocalDate checkAt);

    List<MissionCheck> findAllByUserIdOrderByCheckAtDesc(Long userId);
}
