package com.fitpet.server.mission.domain.exception;

import com.fitpet.server.shared.exception.BusinessException;
import com.fitpet.server.shared.exception.ErrorCode;

public class MissionCheckNotFoundException extends BusinessException {

    public MissionCheckNotFoundException() {
        super(ErrorCode.MISSION_CHECK_NOT_FOUND);
    }
}
