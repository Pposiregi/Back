package com.fitpet.server.mission.application.service;

import com.fitpet.server.mission.domain.entity.Mission;
import com.fitpet.server.mission.domain.entity.UserMissionStat;
import com.fitpet.server.mission.domain.repository.UserMissionStatRepository;
import com.fitpet.server.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserMissionStatCreateService {

    private final UserMissionStatRepository userMissionStatRepository;

    //user_mission_stat 생성은 UNIQUE(user_id, mission_id) 충돌이 발생할 수 있어, 외부 트랜잭션과 분리
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryCreate(User user, Mission mission) {
        try {
            UserMissionStat stat = UserMissionStat.builder()
                    .user(user)
                    .mission(mission)
                    .clearCount(1)
                    .build();
            userMissionStatRepository.save(stat);
            return true;
        } catch (DataIntegrityViolationException ignored) {
            return false;
        }
    }
}

