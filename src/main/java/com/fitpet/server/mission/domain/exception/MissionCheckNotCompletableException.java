package com.fitpet.server.mission.domain.exception;

import com.fitpet.server.shared.exception.BusinessException;
import com.fitpet.server.shared.exception.ErrorCode;

public class MissionCheckNotCompletableException extends BusinessException {

    public MissionCheckNotCompletableException() {
        super(ErrorCode.MISSION_CHECK_NOT_COMPLETABLE);
    }
}
