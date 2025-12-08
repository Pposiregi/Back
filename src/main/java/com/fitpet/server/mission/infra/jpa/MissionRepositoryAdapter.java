package com.fitpet.server.mission.infra.jpa;

import com.fitpet.server.mission.domain.entity.Mission;
import com.fitpet.server.mission.domain.repository.MissionRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MissionRepositoryAdapter implements MissionRepository {

    private final MissionJpaRepository missionJpaRepository;

    @Override
    public Mission save(Mission mission) {
        return missionJpaRepository.save(mission);
    }

    @Override
    public Optional<Mission> findById(Long missionId) {
        return missionJpaRepository.findById(missionId);
    }

    @Override
    public List<Mission> findAll() {
        return missionJpaRepository.findAll();
    }

    @Override
    public void delete(Mission mission) {
        missionJpaRepository.delete(mission);
    }
}
