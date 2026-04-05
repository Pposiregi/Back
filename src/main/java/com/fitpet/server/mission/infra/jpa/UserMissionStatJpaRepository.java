package com.fitpet.server.mission.infra.jpa;

import com.fitpet.server.mission.domain.entity.Mission;
import com.fitpet.server.mission.domain.entity.UserMissionStat;
import com.fitpet.server.user.domain.entity.User;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserMissionStatJpaRepository extends JpaRepository<UserMissionStat, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<UserMissionStat> findWithLockByUserAndMission(User user, Mission mission);

    @Modifying
    @Query(value = """
            insert ignore into user_mission_stat
                (user_id, mission_id, clear_count, created_at, updated_at)
            values
                (:userId, :missionId, 1, now(), now())
            """, nativeQuery = true)
    int insertIfAbsent(@Param("userId") Long userId, @Param("missionId") Long missionId);
}
