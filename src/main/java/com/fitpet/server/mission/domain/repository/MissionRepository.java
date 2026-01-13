package com.fitpet.server.mission.domain.repository;

import com.fitpet.server.mission.domain.entity.Mission;
import com.fitpet.server.mission.domain.entity.MissionType;
import java.util.List;
import java.util.Optional;

public interface MissionRepository {

    Mission save(Mission mission);

    Optional<Mission> findById(Long missionId);

    List<Mission> findAll();

    List<Mission> findByType(MissionType type);

    void delete(Mission mission);
}
