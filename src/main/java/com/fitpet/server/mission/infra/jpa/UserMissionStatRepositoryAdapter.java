package com.fitpet.server.mission.infra.jpa;

import com.fitpet.server.mission.domain.entity.Mission;
import com.fitpet.server.mission.domain.entity.UserMissionStat;
import com.fitpet.server.mission.domain.repository.UserMissionStatRepository;
import com.fitpet.server.user.domain.entity.User;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserMissionStatRepositoryAdapter implements UserMissionStatRepository {

    private final UserMissionStatJpaRepository userMissionStatJpaRepository;

    @Override
    public Optional<UserMissionStat> findWithLockByUserAndMission(User user, Mission mission) {
        return userMissionStatJpaRepository.findWithLockByUserAndMission(user, mission);
    }

    @Override
    public UserMissionStat save(UserMissionStat stat) {
        return userMissionStatJpaRepository.save(stat);
    }

}
