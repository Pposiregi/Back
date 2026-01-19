package com.fitpet.server.mission.infra.jpa;

import com.fitpet.server.mission.domain.entity.MissionCategory;
import com.fitpet.server.mission.domain.entity.MissionCheck;
import com.fitpet.server.mission.domain.entity.MissionType;
import com.fitpet.server.mission.domain.repository.MissionCheckKey;
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
    public Optional<MissionCheck> findByPeriodKey(
        Long missionId,
        Long userId,
        MissionType periodType,
        LocalDate periodStart
    ) {
        return missionCheckJpaRepository.findByPeriodKey(
            missionId,
            userId,
            periodType,
            periodStart
        );
    }

    @Override
    public List<MissionCheckKey> findExistingKeys(
        List<Long> userIds,
        List<Long> missionIds,
        MissionType periodType,
        LocalDate periodStart
    ) {
        return missionCheckJpaRepository.findExistingKeys(userIds, missionIds, periodType, periodStart);
    }

    @Override
    public List<MissionCheck> findRecentByUser(Long userId) {
        return missionCheckJpaRepository.findRecentByUser(userId);
    }

    @Override
    public List<MissionCheck> findActiveByUserAndCategoryAndDate(
        Long userId,
        MissionCategory category,
        LocalDate date
    ) {
        return missionCheckJpaRepository.findActiveByUserAndCategoryAndDate(userId, category, date);
    }

    @Override
    public List<MissionCheck> findActiveByUserAndDate(Long userId, LocalDate date) {
        return missionCheckJpaRepository.findActiveByUserAndDate(userId, date);
    }

    @Override
    public List<MissionCheck> findCompletedByUser(Long userId) {
        return missionCheckJpaRepository.findCompletedByUser(userId);
    }

    @Override
    public void delete(MissionCheck missionCheck) {
        missionCheckJpaRepository.delete(missionCheck);
    }
}
