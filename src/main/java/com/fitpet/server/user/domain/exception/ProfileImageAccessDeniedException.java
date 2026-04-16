package com.fitpet.server.user.domain.exception;

import com.fitpet.server.shared.exception.BusinessException;
import com.fitpet.server.shared.exception.ErrorCode;

public class ProfileImageAccessDeniedException extends BusinessException {

    public ProfileImageAccessDeniedException() {
        super(ErrorCode.PROFILE_IMAGE_ACCESS_DENIED);
    }
}
