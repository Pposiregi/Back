package com.fitpet.server.badge.domain.exception;

import com.fitpet.server.shared.exception.BusinessException;
import com.fitpet.server.shared.exception.ErrorCode;

public class BadgeNotFoundException extends BusinessException {
    public BadgeNotFoundException() {
        super(ErrorCode.BADGE_NOT_FOUND);
    }
}
