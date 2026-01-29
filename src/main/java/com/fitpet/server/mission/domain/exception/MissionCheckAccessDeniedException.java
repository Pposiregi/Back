package com.fitpet.server.mission.domain.exception;

import com.fitpet.server.shared.exception.BusinessException;
import com.fitpet.server.shared.exception.ErrorCode;

public class MissionCheckAccessDeniedException extends BusinessException {

    public MissionCheckAccessDeniedException() {
        super(ErrorCode.MISSION_CHECK_ACCESS_DENIED);
    }
}
