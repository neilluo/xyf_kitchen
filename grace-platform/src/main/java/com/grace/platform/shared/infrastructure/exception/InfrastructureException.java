package com.grace.platform.shared.infrastructure.exception;

public abstract class InfrastructureException extends RuntimeException {
    private final int errorCode;

    protected InfrastructureException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected InfrastructureException(int errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public int getErrorCode() { return errorCode; }
}
