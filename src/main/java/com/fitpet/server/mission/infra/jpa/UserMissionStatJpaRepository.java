package com.fitpet.server.mission.infra.jpa;

import com.fitpet.server.mission.domain.entity.Mission;
import com.fitpet.server.mission.domain.entity.UserMissionStat;
import com.fitpet.server.user.domain.entity.User;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface UserMissionStatJpaRepository extends JpaRepository<UserMissionStat, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<UserMissionStat> findWithLockByUserAndMission(User user, Mission mission);
}
