package com.fitpet.server.mission.infra.jpa;

import com.fitpet.server.mission.domain.entity.MissionCheck;
import com.fitpet.server.mission.domain.repository.MissionCheckRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MissionCheckRepositoryAdapter implements MissionCheckRepository {

    private final MissionCheckJpaRepository missionCheckJpaRepository;

    @Override
    public MissionCheck save(MissionCheck missionCheck) {
        return missionCheckJpaRepository.save(missionCheck);
    }

    @Override
    public Optional<MissionCheck> findById(Long missionCheckId) {
        return missionCheckJpaRepository.findById(missionCheckId);
    }

    @Override
    public Optional<MissionCheck> findByMissionIdAndUserIdAndCheckAt(Long missionId, Long userId, LocalDate checkAt) {
        return missionCheckJpaRepository.findByMissionIdAndUserIdAndCheckAt(missionId, userId, checkAt);
    }

    @Override
    public List<MissionCheck> findAllByUserId(Long userId) {
        return missionCheckJpaRepository.findAllByUserIdOrderByCheckAtDesc(userId);
    }

    @Override
    public void delete(MissionCheck missionCheck) {
        missionCheckJpaRepository.delete(missionCheck);
    }
}
