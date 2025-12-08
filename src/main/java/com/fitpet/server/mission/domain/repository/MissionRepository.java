package com.fitpet.server.mission.domain.repository;

import com.fitpet.server.mission.domain.entity.Mission;
import java.util.List;
import java.util.Optional;

public interface MissionRepository {

    Mission save(Mission mission);

    Optional<Mission> findById(Long missionId);

    List<Mission> findAll();

    void delete(Mission mission);
}
