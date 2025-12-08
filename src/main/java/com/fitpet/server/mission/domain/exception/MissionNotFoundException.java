package com.fitpet.server.mission.domain.exception;

import com.fitpet.server.shared.exception.BusinessException;
import com.fitpet.server.shared.exception.ErrorCode;

public class MissionNotFoundException extends BusinessException {

    public MissionNotFoundException() {
        super(ErrorCode.MISSION_NOT_FOUND);
    }
}
