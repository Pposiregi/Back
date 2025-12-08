package com.fitpet.server.badge.domain.exception;

import com.fitpet.server.shared.exception.BusinessException;
import com.fitpet.server.shared.exception.ErrorCode;

public class BadgeCheckNotFoundException extends BusinessException {
    public BadgeCheckNotFoundException() {
        super(ErrorCode.BADGE_CHECK_NOT_FOUND);
    }
}
