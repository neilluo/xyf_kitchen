package com.grace.platform.shared.infrastructure.exception;

public abstract class DomainException extends RuntimeException {
    private final int errorCode;

    protected DomainException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public int getErrorCode() { return errorCode; }
}
