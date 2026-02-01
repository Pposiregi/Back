package com.fitpet.server.mission.domain.repository;

import com.fitpet.server.mission.domain.entity.MissionCheck;
import com.fitpet.server.mission.domain.entity.MissionCategory;
import com.fitpet.server.mission.domain.entity.MissionType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MissionCheckRepository {

    MissionCheck save(MissionCheck missionCheck);

    List<MissionCheck> saveAll(List<MissionCheck> missionChecks);

    Optional<MissionCheck> findById(Long missionCheckId);

    Optional<MissionCheck> findByIdForUpdate(Long missionCheckId);

    Optional<MissionCheck> findByPeriodKey(
        Long missionId,
        Long userId,
        MissionType periodType,
        LocalDate periodStart
    );

    List<MissionCheckKey> findExistingKeys(
        List<Long> userIds,
        List<Long> missionIds,
        MissionType periodType,
        LocalDate periodStart
    );

    List<MissionCheck> findRecentByUser(Long userId);

    List<MissionCheck> findActiveByUserAndCategoryAndDate(
        Long userId,
        MissionCategory category,
        LocalDate date
    );

    List<MissionCheck> findActiveByUserAndDate(Long userId, LocalDate date);

    List<MissionCheck> findCompletedByUser(Long userId);

    void delete(MissionCheck missionCheck);
}
