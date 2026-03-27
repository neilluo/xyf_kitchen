package com.grace.platform.shared.infrastructure.exception;

import com.grace.platform.shared.ErrorCode;

public class EncryptionException extends InfrastructureException {
    public EncryptionException(String message, Throwable cause) {
        super(ErrorCode.ENCRYPTION_ERROR, message, cause);
    }
}
