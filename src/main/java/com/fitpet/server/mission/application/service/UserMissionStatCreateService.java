package com.fitpet.server.mission.application.service;

import com.fitpet.server.mission.domain.entity.Mission;
import com.fitpet.server.mission.domain.repository.UserMissionStatRepository;
import com.fitpet.server.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
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
        return userMissionStatRepository.insertIfAbsent(user, mission);
    }
}
