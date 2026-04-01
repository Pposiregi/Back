package com.fitpet.server.mission.domain.repository;

import com.fitpet.server.mission.domain.entity.Mission;
import com.fitpet.server.mission.domain.entity.UserMissionStat;
import com.fitpet.server.user.domain.entity.User;
import java.util.Optional;

public interface UserMissionStatRepository {

    Optional<UserMissionStat> findWithLockByUserAndMission(User user, Mission mission);

    boolean insertIfAbsent(User user, Mission mission);

    UserMissionStat save(UserMissionStat stat);
}
