package com.fitpet.server.auth.domain.exception;

import com.fitpet.server.shared.exception.BusinessException;
import com.fitpet.server.shared.exception.ErrorCode;

public class OAuthProviderMismatchException extends BusinessException {
    public OAuthProviderMismatchException() {
        super(ErrorCode.OAUTH_PROVIDER_MISMATCH);
    }
}
