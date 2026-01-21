package com.fitpet.server.mission.application.service;

import com.fitpet.server.mission.domain.entity.MissionCheck;
import com.fitpet.server.mission.domain.repository.MissionCheckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MissionCheckBatchSaveService {

    private final MissionCheckRepository missionCheckRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveInNewTx(MissionCheck missionCheck) {
        missionCheckRepository.save(missionCheck);
    }
}
