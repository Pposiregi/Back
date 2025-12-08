package com.fitpet.server.mission.domain.repository;

import com.fitpet.server.mission.domain.entity.MissionCheck;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MissionCheckRepository {

    MissionCheck save(MissionCheck missionCheck);

    Optional<MissionCheck> findById(Long missionCheckId);

    Optional<MissionCheck> findByMissionIdAndUserIdAndCheckAt(Long missionId, Long userId, LocalDate checkAt);

    List<MissionCheck> findAllByUserId(Long userId);

    void delete(MissionCheck missionCheck);
}
